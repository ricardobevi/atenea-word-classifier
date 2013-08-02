package org.squadra.atenea.wordclassifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class WordClassifier {

	private List<String> classifiers;

	private Document doc;

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) {

		WordClassifier WC = new WordClassifier();

		WC.classifyWord("siempre");

	}

	WordClassifier() {

		classifiers = new ArrayList<String>();

		classifiers.add("adjetivo");

	}

	public void classifyWord(String word) {

		boolean isVerb = false;
		String suggestedLink;
		Elements content = new Elements();

		try {

			doc = Jsoup.connect(
					"http://lema.rae.es/drae/srv/search?val=" + word).get();

			// Obtenemos las clasificaciones y alguna basura mas
			content = doc.getElementsByClass("d");

			if (content.isEmpty()) {

				// No existe la palabra, se suguiere otra

				suggestedLink = doc.getElementsByTag("a").get(0).attr("href");

				doc = Jsoup.connect(
						"http://lema.rae.es/drae/srv/" + suggestedLink).get();
				
				// Obtenemos las clasificaciones y alguna basura mas
				content = doc.getElementsByClass("d");

			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		// por ahora lo mostramos, falta ver cuales de estos nos sirven
		for (Element title : content) {

			if (title.attr("title").indexOf("verbo") > -1) {
				isVerb = true;
			}

			System.out.println(title.attr("title"));

		}

		if (isVerb) {
			System.out.println("----Es verbo:---- ");
			conjugate();
		}

	}

	private void conjugate() {

		String conjugationURL;
		conjugationURL = getConjugationURL();
		String url = "http://lema.rae.es/drae/srv/" + conjugationURL;

		Document conjugationDoc = null;

		try {

			conjugationDoc = Jsoup.connect(url).get();

		} catch (IOException e) {
			e.printStackTrace();
		}

		Elements blocks = conjugationDoc.getElementsByClass("x");

		for (Element block : blocks) {

			String tense = block.getElementsByClass("y").html();

			String conjugatedVerbs = block.getElementsByClass("z").html();

			String[] conjugatedVerb = conjugatedVerbs.split("<br />");

			System.out.println("tiempo:" + tense + ":" + conjugatedVerb[0]);

		}

	}

	private String getConjugationURL() {

		String conjugationURL = new String();

		Elements conjugationURLElements = doc.select("a[target=_self]");

		for (Element conjugationElement : conjugationURLElements) {

			if (conjugationElement.child(0).attr("alt")
					.equals("Ver conjugación")) {

				conjugationURL = conjugationElement.attr("href");

			}

		}

		return conjugationURL;
	}

	/*
	 * 
	 * private List<String> searchClassifiers(String word) {
	 * System.out.println("Searching classifier for " + word);
	 * 
	 * List returnClassifiers = new ArrayList();
	 * 
	 * return returnClassifiers;
	 * 
	 * }
	 */

}
