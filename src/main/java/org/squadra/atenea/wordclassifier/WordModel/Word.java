package org.squadra.atenea.wordclassifier.WordModel;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import com.google.gson.Gson;

public class Word {

	private String word;
	private String gender;
	private String number;
	private HashSet<String> classification;

	// vacio si no es un verbo
	private HashMap<String, ArrayList<String>> conjugations;

	public Word(String word) {
		this.word = word;
		this.gender = "";
		this.number = "";
		this.classification = new HashSet<String>();
		this.conjugations = new HashMap<String, ArrayList<String>>();
	}

	public HashMap<String, ArrayList<String>> getConjugations() {
		return conjugations;
	}

	public void setConjugations(HashMap<String, ArrayList<String>> conjugations) {
		this.conjugations = conjugations;
	}

	public String getWord() {
		return word;
	}

	public void setWord(String word) {
		this.word = word;
	}

	public String getMainType() {
		return classification.iterator().next();
	}

	public HashSet<String> getClassification() {
		return classification;
	}

	public void setClassification(HashSet<String> classification) {
		this.classification = classification;
	}

	public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

	public String toString() {
		Gson gson = new Gson();
		String ret = "";

		ret = gson.toJson(this);

		return ret;
	}

}
