package org.squadra.atenea.wordclassifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.squadra.atenea.wordclassifier.WordModel.Word;

/*
 * TODO:
 * - obtener clasificaciones
 * - obtener clasificaciones si es verbo
 * - crear objeto palabra
 * - paralelizar lo paralelizable
 * 
 */

public class WordClassifier {

	private HashSet<String> classifiers;

	// private Document doc;

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) {

		WordClassifier WC = new WordClassifier();

		Word result = WC.classifyWord("correr");

		System.out.println(result);

	}

	public WordClassifier() {

		classifiers = new HashSet<String>();

		classifiers.add("adjetivo");
		classifiers.add("verbo");
		classifiers.add("interjección");
		classifiers.add("nombre");

	}

	public Word classifyWord(String word) {

		Word classifiedWord = new Word(word);

		boolean isVerb = false;

		Document doc = null;
		Elements content = new Elements();
		String suggestedLink;

		ArrayList<String> classifications = new ArrayList<String>();

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

		for (Element title : content) {

			// Si ya se que es verbo no hace falta seguir preguntando
			if (isVerb == false && title.attr("title").indexOf("verbo") > -1) {
				isVerb = true;
			}

			String[] spplitedTitle = title.attr("title").split(" ");

			for (String titleWord : spplitedTitle) {
				// Busco para encontrar el genero

				if (classifiedWord.getGender().equals("") && 
						(titleWord.equals("masculino") || titleWord.equals("fememino"))) {

					classifiedWord.setGender(titleWord);

				} else {

					classifications.add(titleWord);

				}

			}

		}

		classifiedWord.setClassification(this
				.searchClassifiers(classifications));

		if (isVerb) {
			System.out.println("----Es verbo:---- ");
			classifiedWord.setConjugations(conjugate(doc));
		}

		return classifiedWord;

	}

	private HashMap<String, ArrayList<String>> conjugate(Document doc) {

		HashMap<String, ArrayList<String>> conjugations = new HashMap<String, ArrayList<String>>();

		String conjugationURL;
		conjugationURL = getConjugationURL(doc);
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

			String rawConjugations = block.getElementsByClass("z").html();

			ArrayList<String> conjugatedVerbs = new ArrayList<String>();

			String[] splittedRawConjugations = rawConjugations.split("<br />");

			for (Integer i = 0; i < splittedRawConjugations.length; i++) {
				conjugatedVerbs.add(splittedRawConjugations[i]);
			}

			conjugations.put(tense, conjugatedVerbs);

		}

		return conjugations;

	}

	private String getConjugationURL(Document doc) {

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

	private HashSet<String> searchClassifiers(ArrayList<String> classifications) {

		HashSet<String> returnClassifiers = new HashSet<String>();

		for (String classification : classifications) {

			if (this.classifiers.contains(classification)) {
				returnClassifiers.add(classification);
			}

		}

		return returnClassifiers;

	}

}
