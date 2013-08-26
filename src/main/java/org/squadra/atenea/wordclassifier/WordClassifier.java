package org.squadra.atenea.wordclassifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.squadra.atenea.base.word.Word;
import org.squadra.atenea.base.word.WordTypes;

import com.google.gson.Gson;

/*
 * TODO:
 * - obtener clasificaciones
 * - obtener clasificaciones si es verbo
 * - crear objeto palabra
 * - paralelizar lo paralelizable
 * 
 */

public class WordClassifier {


	// private Document doc;

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) {

		System.out.println("INICIANDO...");
		
		Gson gson = new Gson();
		
		WordClassifier WC = new WordClassifier();

		List<Word> results = WC.classifyWord("correr");

		System.out.println("Resultados:");
		//System.out.println(gson.toJson(results));
		System.out.println(results.toString());

	}

	public List<Word> classifyWord(String word) {

		boolean isVerb = false;

		Document doc = null;
		Elements content = new Elements();
		String suggestedLink;

		// Obtenemos la definicion de la RAE y si no coincide la palabra
		// buscamos una segerencia
		ArrayList<String> classifications = new ArrayList<String>();

		try {

			doc = Jsoup.connect(
					"http://lema.rae.es/drae/srv/search?val=" + word).get();

			// Obtenemos las clasificaciones y alguna basura mas
			content = doc.getElementsByClass("d");

			if (content.isEmpty()) {

				// TODO: deberia traer varias sugerencias o ver si la que trae
				// es igual
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

		// Clasificamos que tipo de palabra es

		for (Element title : content) {

			// Si ya se que es verbo no hace falta seguir preguntando
			if (isVerb == false && title.attr("title").indexOf("verbo") > -1) {
				isVerb = true;
			}

			String[] spplitedTitle = title.attr("title").split(" ");

			for (String titleWord : spplitedTitle) {

				/*
				 * // Busco para encontrar el genero if
				 * (classifiedWord.getGender().equals("") ) { if
				 * (titleWord.equals("masculino") ) {
				 * 
				 * classifiedWord.setGender( WordTypes.Gender.MALE );
				 * 
				 * } else if( titleWord.equals("fememino") ) {
				 * 
				 * classifiedWord.setGender( WordTypes.Gender.FAMALE ); }
				 * 
				 * 
				 * } else {
				 * 
				 * classifications.add(titleWord);
				 * 
				 * }
				 */

				classifications.add(titleWord);

			}

		}

		// Creo lista de Words para devolver

		ArrayList<Word> words = new ArrayList<Word>();

		for (String classification : classifications) {

			if (classification.equals("nombre")) {

				words.add(new Word(word, WordTypes.Type.NOUN));

			} else if (classification.equals("adjetivo")) {

				words.add(new Word(word, WordTypes.Type.ADJECTIVE));

			} else if (classification.equals("interjección")) {

				words.add(new Word(word, WordTypes.Type.INTERJECTION));

			} else if (classification.equals("verbo")) {

				ArrayList<Word> conjugatedVerb = conjugate(doc, word);
		
				words.addAll(conjugatedVerb);
			
			}

		}

		return words;

	}

	/*************************************************************************/
	/* obtengo las conjugaciones para un verbo */
	
	private ArrayList<Word> conjugate(Document doc, String baseWord) {

		ArrayList<Word> conjugatedVerbs = new ArrayList<Word>();

		String conjugationURL;
		conjugationURL = getConjugationURL(doc);
		String url = "http://lema.rae.es/drae/srv/" + conjugationURL;
	
		Document conjugationDoc = null;

		try {

			conjugationDoc = Jsoup.connect(url).get();

		} catch (IOException e) {
			e.printStackTrace();
		}

		// Tomo modo indicativo y subjuntivo
		
		Elements blocks = conjugationDoc.getElementsByClass("x");
		
		for (int indexBlock = 0 ; indexBlock < blocks.size() ; indexBlock++) {	

			Element block = blocks.get(indexBlock);
			
			String tense = block.getElementsByClass("y").html();

			String rawConjugations = block.getElementsByClass("z").html();

			String[] splittedRawConjugations = rawConjugations.split("<br />");

			for (Integer i = 0; i < splittedRawConjugations.length; i++) {
					
				String [] splittedConjugations =  splittedRawConjugations[i].split("/| o ");
							
				for( Integer j = 0;  j < splittedConjugations.length  ; j++ ){
					
					Word auxWord = new Word();
					
					auxWord.setTense( WordTypes.getTenseRaeClasification(tense)  );
					
					auxWord.setBaseWord(baseWord);
					
					auxWord.setPerson( WordTypes.getPersonByNumber(i) );
					
					auxWord.setType( WordTypes.Type.VERB  ); 
					
					auxWord.setName( splittedConjugations[j]);
					
					auxWord.setMode( ((indexBlock + 1) % 3) == 0 ?  WordTypes.Mode.SUBJUNCTIVE : WordTypes.Mode.INDICATIVE  );
					
					conjugatedVerbs.add(auxWord);
				}
				
			}

		}
		
		// Ahora tomo el modo imperativo
		
		Elements imperativeConjugationsBlock = conjugationDoc.getElementsByClass("r");
		
		Elements imperativeConjugationVerbs = imperativeConjugationsBlock.first().getElementsByClass("z");
		
		String imperativeConjugationVerb = imperativeConjugationVerbs.first().html();
		
		String[] splittedImperativeConjugationsVerbs = imperativeConjugationVerb.split("<br />|/");
		
		for( Integer k = 0; k < splittedImperativeConjugationsVerbs.length  ; k++ ){
			
			Word auxWord = new Word();
			
			auxWord.setBaseWord(baseWord);
			
			String[] auxImperativeVerb = splittedImperativeConjugationsVerbs[k].trim().split(" ");
			
			auxWord.setPerson( WordTypes.getPersonByImperativeRaeDefinition(auxImperativeVerb[1]) );
			
			auxWord.setType( WordTypes.Type.VERB  ); 
			
			auxWord.setName( auxImperativeVerb[0] );
			
			auxWord.setMode(  WordTypes.Mode.IMPERATIVE  );
			
			conjugatedVerbs.add(auxWord);
		}
		
		
		return conjugatedVerbs;

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

}
