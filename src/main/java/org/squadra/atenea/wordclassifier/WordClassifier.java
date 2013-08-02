package org.squadra.atenea.wordclassifier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class WordClassifier {

	private static int bufferLen = 1024;

	private String wordToClassify;

	private URL raeUrl;

	private HttpURLConnection conn;

	private InputStream webIS;

	private BufferedReader web;

	private List<String> classifiers;

	private char[] buffer;

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) {

		WordClassifier WC = new WordClassifier();

		WC.classifyWord("Puto");

	}

	WordClassifier() {
		buffer = new char[bufferLen];

		classifiers = new ArrayList();

		classifiers.add("adjetivo");
	}

	public void classifyWord(String word) {

		try {

			String wordToClassify;

			System.out.println("Classifying word " + word);

			wordToClassify = URLEncoder.encode(word.toLowerCase(), "UTF-8");

			raeUrl = new URL("http://lema.rae.es/drae/srv/search?val="
					+ wordToClassify);

			System.out.println("getting web");

			conn = (HttpURLConnection) raeUrl.openConnection();

			conn.setConnectTimeout(30000000);

			conn.setReadTimeout(30000000);

			web = new BufferedReader(new InputStreamReader(
					conn.getInputStream()));

			System.out.println("done!");

			String wordBuffer = new String();

			while (web.read(buffer) > 0) {

				for (Integer i = 0; i < bufferLen; i++) {
					wordBuffer = wordBuffer + buffer[i];
				}
				
				// aca hay que limpiar la variable buffer, pero evitar crear un nuevo char
				buffer = new char[bufferLen];
			}
			
			
			//<span class="d" title="adjetivo">adj.</span>
			//System.out.println( wordBuffer  );
			
			Integer i = 0;
			
			Integer wordBufferLen = wordBuffer.length();
			
			String classification = "";
			
			List<String> classifications = new ArrayList<String>();;
			
			while( i <  wordBufferLen )
			{
				while( i <  wordBufferLen  && wordBuffer.charAt( i++ ) != '"' );
				
				if( i <  wordBufferLen  && i+10 <  wordBufferLen  
					&&   wordBuffer.substring( i , i+10 ).equals("d\" title=\"") )
				{
					i = i + 10;
					
					//while( i <  wordBufferLen  && wordBuffer.charAt( i++ ) != '"' ) ;
					
					while( i <  wordBufferLen  && wordBuffer.charAt( i ) != '"' )
					{
						classification += wordBuffer.charAt( i );
						
						i++;
					}
					
					if( classification != "" )
					{
						classifications.add(classification);
						classification = "";
					}
					
					
				}
				
			}
		
			
			System.out.println("Classifications:");
			
			for (String element : classifications) {
				System.out.println(element);
			}
			

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private List<String> searchClassifiers(String word) {
		System.out.println("Searching classifier for " + word);

		List returnClassifiers = new ArrayList();

		return returnClassifiers;

	}

}
