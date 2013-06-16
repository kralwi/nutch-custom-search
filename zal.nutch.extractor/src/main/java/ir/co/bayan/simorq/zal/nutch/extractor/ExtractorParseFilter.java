package ir.co.bayan.simorq.zal.nutch.extractor;

import ir.co.bayan.simorq.zal.nutch.extractor.config.SelectorConfiguration;

import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.log4j.Logger;
import org.apache.nutch.metadata.Metadata;
import org.apache.nutch.parse.HTMLMetaTags;
import org.apache.nutch.parse.HtmlParseFilter;
import org.apache.nutch.parse.ParseResult;
import org.apache.nutch.protocol.Content;
import org.w3c.dom.DocumentFragment;

public class ExtractorParseFilter implements HtmlParseFilter {

	private static final Logger LOGGER = Logger.getLogger(ExtractorParseFilter.class);

	private Configuration configuration;
	private Extractor extractor;
	private String defaultEncoding;

	@Override
	public ParseResult filter(Content content, ParseResult parseResult, HTMLMetaTags metaTags,
			DocumentFragment documentFragment) {
		try {
			Metadata metadata = parseResult.get(content.getUrl()).getData().getParseMeta();
			String encoding = StringUtils.defaultString(metadata.get(Metadata.ORIGINAL_CHAR_ENCODING), defaultEncoding);
			List<ExtractedDoc> extractedDocs = extractor.extract(content.getUrl(), content.getContent(), encoding,
					content.getContentType());
			if (extractedDocs != null) {
				for (ExtractedDoc doc : extractedDocs) {
					Metadata docMetadata = parseResult.get(doc.getUrl()).getData().getParseMeta();
					// Indicates that this document is matched with one of urls defined in the config.
					// This will be used in ParseMetadataIndexingFilter to decide whether exclude document or not
					docMetadata.add(ExtractorIndexingFilter.MATCHED_DOC, "true");
					for (Entry<String, String> entry : doc.getFields().entrySet()) {
						docMetadata.add(entry.getKey(), entry.getValue());
					}

				}
			}

		} catch (Exception e) {
			LOGGER.warn("", e);
		}

		return parseResult;
	}

	@Override
	public Configuration getConf() {
		return configuration;
	}

	@Override
	public void setConf(Configuration configuration) {
		this.configuration = configuration;
		defaultEncoding = configuration.get("parser.character.encoding.default", "UTF-8");
		try {
			initConf();
		} catch (Exception e) {
			LOGGER.error("", e);
		}

	}

	private void initConf() throws Exception {
		extractor = new Extractor(SelectorConfiguration.readConfig(configuration));
	}

}