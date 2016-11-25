package lu.tessyglodt.site;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import lu.tessyglodt.site.data.Page;

@XmlRootElement
public class Import {

	final static Logger logger = LoggerFactory.getLogger(Import.class);

	private static String readFile(final String path) throws IOException {
		final byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, StandardCharsets.UTF_8);
	}

	// public static void main(final String[] args) throws
	// ParserConfigurationException, SAXException, IOException {
	// doImport();
	// }

	public static List<Page> doImport() throws ParserConfigurationException, SAXException, IOException {

		final File dir = new File("/home/yves/projects/tessyglodt.lu/opencms");
		final File[] directoryListing = dir.listFiles();
		final List<Page> pages = new ArrayList<Page>();
		final Date now = new Date();

		if (directoryListing != null) {

			Arrays.sort(directoryListing);
			int i = 1;
			for (final File file : directoryListing) {

				final String fileName = "/home/yves/projects/tessyglodt.lu/opencms/" + file.getName();

				final String pageName = StringUtils.capitalize(file.getName().substring(0, file.getName().length() - 9));
				/*
				 * final String pageName = file.getName().substring(0,
				 * file.getName().length() - 9);
				 */

				final File fXmlFile = new File(fileName);
				final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				final Document doc = dBuilder.parse(fXmlFile);

				doc.getDocumentElement().normalize();

				final NodeList nList = doc.getElementsByTagName("Article");

				for (int temp = 0; temp < nList.getLength(); temp++) {

					final Node nNode = nList.item(temp);

					if (nNode.getNodeType() == Node.ELEMENT_NODE) {

						final Element e = (Element) nNode;

						final Page page = new Page();
						page.setId(UUID.randomUUID().toString().replace("-", ""));

						page.setTitle(e.getElementsByTagName("Title").item(0).getTextContent());
						// page.setCanonicalName(e.getElementsByTagName("CanonicalName").item(0).getTextContent());

						page.setName(pageName);

						// logger.debug(page.toString());

						page.setDatePublished(e.getElementsByTagName("DateBroadcasted").item(0).getTextContent());

						page.setLatitude(new BigDecimal(e.getElementsByTagName("Latitude").item(0).getTextContent().trim()));
						page.setLongitude(new BigDecimal(e.getElementsByTagName("Longitude").item(0).getTextContent().trim()));

						final Element text = (Element) e.getElementsByTagName("Text").item(0);

						page.setContent(text.getElementsByTagName("content").item(0).getTextContent());

						final Element e0 = (Element) e.getElementsByTagName("Categories").item(0);
						final Element e1 = (Element) e.getElementsByTagName("Categories").item(1);
						final Element e2 = (Element) e.getElementsByTagName("Categories").item(2);

						final String[] comm = e0.getTextContent().split("/");
						final String[] cann = e1.getTextContent().split("/");
						final String[] diss = e2.getTextContent().split("/");

						// System.out.println(diss[diss.length - 1] + " " +
						// cann[cann.length - 1] + " " + comm[comm.length - 1]);

						// System.out.println(comm[comm.length - 1]);

						// page.setCname(comm[comm.length - 1]);

						// System.out.println("'" + diss[diss.length - 1] +
						// "' '" + cann[cann.length - 1] + "'");

						// System.out.println(can.getTextContent() + " " +
						// com.getTextContent() + " " + dis.getTextContent());

						// page.setOpenCmsContent(readFile(fileName));

						page.setDateCreated(LocalDateTime.now());
						page.setPublished(true);
						/*
						 * switch (diss[diss.length - 1]) { case "Diekirch":
						 * page.setDistrict(new District(1)); break; case
						 * "Grevenmacher": page.setDistrict(new District(2));
						 * break; case "Luxembourg": page.setDistrict(new
						 * District(3)); break; default: break; }
						 * switch (cann[cann.length - 1]) { case "Capellen":
						 * page.setCanton(new Canton(1)); break; case
						 * "Clervaux": page.setCanton(new Canton(2)); break;
						 * case "Diekirch": page.setCanton(new Canton(3));
						 * break; case "Echternach": page.setCanton(new
						 * Canton(4)); break; case "Esch-sur-Alzette":
						 * page.setCanton(new Canton(5)); break; case
						 * "Grevenmacher": page.setCanton(new Canton(6)); break;
						 * case "Luxembourg": page.setCanton(new Canton(7));
						 * break; case "Mersch": page.setCanton(new Canton(8));
						 * break; case "Redange": page.setCanton(new Canton(9));
						 * break; case "Remich": page.setCanton(new Canton(10));
						 * break; case "Vianden": page.setCanton(new
						 * Canton(11)); break; case "Wiltz": page.setCanton(new
						 * Canton(12)); break; default: break; }
						 */
						// System.out.println(i + ": " + page.getName() + " " +
						// page.getCanton() + " " + cann[cann.length - 1]);
						// System.out.println(i + ": " + page.getName() + " " +
						// page.getDistrict() + " " + diss[diss.length - 1]);

						/*
						 * 1 Kapellen 2 Klierf 3 Dikrech 4 Iechternach 5
						 * Esch-Uelzecht 6 Gréiwemaacher 7 Lëtzebuerg 8 Miersch
						 * 9 Réiden 10 Réimech 11 Veianen 12 Wolz
						 * 1 Dikrech 2 Gréiwemaacher 3 Lëtzebuerg
						 */

						// System.out.println(i + ": " + page.toString());

						pages.add(page);

					}
				}

				i++;

				// break;
			}
		}
		return pages;
	}
}
