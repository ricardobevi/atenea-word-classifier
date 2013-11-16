package org.squadra.atenea.wordclassifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.log4j.Log4j;

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
@Log4j
public class WordClassifier {


	// private Document doc;

	public static void main(String[] args) {

		System.out.println("INICIANDO...");
		
		//Gson gson = new Gson();
		
		//WordClassifier WC = new WordClassifier();
		
		//System.out.println( gson.toJson( WC.isImperative("jugo"),) );

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
					"http://lema.rae.es/drae/srv/search?val=" + word).timeout(10000).get();

			// Obtenemos las clasificaciones y alguna basura mas
			content = doc.getElementsByClass("d");

			if (content.isEmpty()) {

				// TODO: deberia traer varias sugerencias o ver si la que trae
				// es igual
				// No existe la palabra, se suguiere otra

				suggestedLink = doc.getElementsByTag("a").get(0).attr("href");

				doc = Jsoup.connect(
						"http://lema.rae.es/drae/srv/" + suggestedLink).timeout(10000).get();

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
	
	private ArrayList<Word> conjugate(Document doc, String baseWord) {

		ArrayList<Word> conjugatedVerbs = new ArrayList<Word>();

		String conjugationURL;
		conjugationURL = getConjugationURL(doc);
		String url = "http://lema.rae.es/drae/srv/" + conjugationURL;
	
		Document conjugationDoc = null;

		try {

			conjugationDoc = Jsoup.connect(url).timeout(10000).get();

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
	
	public boolean isImperative( String word , String baseVerb ){

		Boolean isVerb = false;
		Boolean result = false;
		
		Document doc = null;
		Elements content = new Elements();
		String suggestedLink;

		try {

			doc = Jsoup.connect(
					"http://lema.rae.es/drae/srv/search?val=" + baseVerb).timeout(10000).get();

			// Obtenemos las clasificaciones y alguna basura mas
			content = doc.getElementsByClass("d");

			if (content.isEmpty()) {

				// TODO: deberia traer varias sugerencias o ver si la que trae
				// es igual
				// No existe la palabra, se suguiere otra

				
				if( doc.getElementsByTag("a") != null  && 
					doc.getElementsByTag("a").get(0) != null &&  
					doc.getElementsByTag("a").get(0).attr("href") != null){
					
					suggestedLink = doc.getElementsByTag("a").get(0).attr("href");

					doc = Jsoup.connect(
							"http://lema.rae.es/drae/srv/" + suggestedLink).timeout(10000).get();

					// Obtenemos las clasificaciones y alguna basura mas
					content = doc.getElementsByClass("d");

					
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		if( content.isEmpty() ){
		
			// Clasificamos que tipo de palabra es
			
			
			ArrayList<String> imperativeVerbs  = new ArrayList<String>();
			
			for (Element title : content) {

				if (isVerb == false && title.attr("title").indexOf("verbo") > -1) {
					isVerb = true;
					imperativeVerbs = getImpratives(doc, word);
				}

			}
			
			Gson gson = new Gson();
				
			log.debug("---------Imperativos--------- busco:" + word + " a traves de " + baseVerb + " En:" + gson.toJson(imperativeVerbs) );
			
			result = imperativeVerbs.contains(word); 
		}
				
		return result; 
	}

	private ArrayList<String> addImperativeDerived(ArrayList<String> imperativeVerbs) {
		
		ArrayList<String> allImperativeVerbs = new ArrayList<String>();
		
		for (String imperativeVerb : imperativeVerbs) {
			
			allImperativeVerbs.add(imperativeVerb);
			allImperativeVerbs.add(imperativeVerb + "me");
			allImperativeVerbs.add(imperativeVerb + "te");
			allImperativeVerbs.add(imperativeVerb + "nos");
			allImperativeVerbs.add(imperativeVerb + "les");
			allImperativeVerbs.add(imperativeVerb + "le");
			
		}
		
		return allImperativeVerbs;
		
		
	}

	private ArrayList<String> getImpratives(Document doc, String baseWord) {
	
		ArrayList<String> imperativeVerbs = new ArrayList<String>();

		String conjugationURL;
		conjugationURL = getConjugationURL(doc);
		String url = "http://lema.rae.es/drae/srv/" + conjugationURL;
	
		Document conjugationDoc = null;

		try {

			conjugationDoc = Jsoup.connect(url).timeout(10000).get();

		} catch (IOException e) {
			e.printStackTrace();
		}

		// Ahora tomo el modo imperativo
		Elements imperativeConjugationsBlock = conjugationDoc.getElementsByClass("r");
		Elements imperativeConjugationVerbs = imperativeConjugationsBlock.first().getElementsByClass("z");
		String imperativeConjugationVerb = imperativeConjugationVerbs.first().html();
		String[] splittedImperativeConjugationsVerbs = imperativeConjugationVerb.split("<br />|/");
		
		for( Integer k = 0; k < splittedImperativeConjugationsVerbs.length  ; k++ ){
			String[] auxImperativeVerb = splittedImperativeConjugationsVerbs[k].trim().split(" "); 

			//TODO: Para que tome los imperativos con acentos se debe salvar el unicode de la siguiente linea
			imperativeVerbs.add( decodeUnicodeAccent(auxImperativeVerb[0]) );
		}
		
		imperativeVerbs = addImperativeDerived(imperativeVerbs);
		
		return imperativeVerbs;

	}
	
	private String decodeUnicodeAccent( String word ){
		
		word.replace("\u00C1", "Á" );
		word.replace("\u00E1", "á" );
		word.replace("\u00C9", "É" );
		word.replace("\u00E9", "é" );
		word.replace("\u00CD", "Í" );
		word.replace("\u00ED", "í" );
		word.replace("\u00D3", "Ó" );
		word.replace("\u00F3", "ó" );
		word.replace("\u00DA", "Ú" );
		word.replace("\u00FA", "ú" );
		
		return word;
		
	}

}
