package org.verapdf.gf.model;

import com.adobe.xmp.XMPException;
import com.adobe.xmp.impl.VeraPDFMeta;
import org.verapdf.core.EncryptedPdfException;
import org.verapdf.core.ModelParsingException;
import org.verapdf.exceptions.InvalidPasswordException;
import org.verapdf.features.FeatureExtractorConfig;
import org.verapdf.features.FeaturesExtractor;
import org.verapdf.features.tools.FeaturesCollection;
import org.verapdf.gf.model.impl.containers.StaticContainers;
import org.verapdf.gf.model.impl.cos.GFCosDocument;
import org.verapdf.metadata.fixer.entity.PDFDocument;
import org.verapdf.pd.PDCatalog;
import org.verapdf.pd.PDDocument;
import org.verapdf.pd.PDMetadata;
import org.verapdf.pdfa.PDFParser;
import org.verapdf.pdfa.flavours.PDFAFlavour;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Timur Kamalov
 */
public class GFModelParser implements PDFParser, Closeable {

	private static final Logger LOGGER = Logger.getLogger(GFModelParser.class.getCanonicalName());

	private PDDocument document;

	private final PDFAFlavour flavour;

	private GFModelParser(final InputStream docStream, PDFAFlavour flavour) throws IOException {
		this.document = new PDDocument(docStream);
		this.flavour = (flavour == PDFAFlavour.NO_FLAVOUR) ? obtainFlavour(this.document) : flavour;
		initializeStaticContainers(this.document, this.flavour);
	}

	public static GFModelParser createModelWithFlavour(InputStream toLoad, PDFAFlavour flavour)
			throws ModelParsingException, EncryptedPdfException {
		try {
			return new GFModelParser(toLoad, flavour);
		} catch (InvalidPasswordException excep) {
			throw new EncryptedPdfException("The PDF stream appears to be encrypted.", excep);
		} catch (IOException e) {
			throw new ModelParsingException("Couldn't parse stream", e);
		}
	}

	private static PDFAFlavour obtainFlavour(PDDocument document) {
		PDMetadata metadata = null;
		try {
			if (document == null || document.getCatalog() == null) {
				return PDFAFlavour.NO_FLAVOUR;
			}
			metadata = document.getCatalog().getMetadata();
			if (metadata == null) {
				return PDFAFlavour.NO_FLAVOUR;
			}
		} catch (IOException e) {
			LOGGER.log(Level.FINE, "Problem parsing metadata from document catalog.", e);
			return PDFAFlavour.NO_FLAVOUR;
		}
		try (InputStream is = metadata.getStream()) {
			VeraPDFMeta veraPDFMeta = VeraPDFMeta.parse(metadata.getStream());
			Integer identificationPart = veraPDFMeta.getIdentificationPart();
			String identificationConformance = veraPDFMeta.getIdentificationConformance();
			PDFAFlavour pdfaFlavour = PDFAFlavour.byFlavourId(identificationPart + identificationConformance);
			return pdfaFlavour;
		} catch (IOException | XMPException e) {
			LOGGER.log(Level.FINE, e.getMessage(), e);
			return PDFAFlavour.NO_FLAVOUR;
		}
	}

	private static void initializeStaticContainers(final PDDocument document, final PDFAFlavour flavour) {
		StaticContainers.clearAllContainers();
		StaticContainers.setDocument(document);
		StaticContainers.setFlavour(flavour);
	}

	/**
	 * Get {@code PDDocument} object for current file.
	 *
	 * @return {@link org.verapdf.pd.PDDocument} object of greenfield library.
	 * @throws IOException
	 *             when target file is not pdf or pdf file is not contain root
	 *             object
	 */
	public PDDocument getPDDocument() throws IOException {
		return this.document;
	}

	/**
	 * Method return root object of model implementation from greenfield model
	 * together with the hierarchy.
	 *
	 * @return root object representing by
	 *         {@link org.verapdf.model.coslayer.CosDocument}
	 * @throws IOException
	 *             when target file is not pdf or pdf file is not contain root
	 *             object
	 */
	@Override
	public org.verapdf.model.baselayer.Object getRoot() {
		return new GFCosDocument(this.document.getDocument());
	}

	@Override
	public PDFAFlavour getFlavour() {
		return this.flavour;
	}

	@Override
	public PDFDocument getPDFDocument() {
		// TODO: implement me with metadata fixer
		return null;
	}

	@Override
	public FeaturesCollection getFeatures(FeatureExtractorConfig config) {
		return null;
	}

	@Override
	public FeaturesCollection getFeatures(FeatureExtractorConfig config, List<FeaturesExtractor> extractors) {
		return null;
	}

	@Override
	public void close() {
		if (this.document != null) {
			this.document.close();
		}
	}

}
