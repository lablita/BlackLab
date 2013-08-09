/*******************************************************************************
 * Copyright (c) 2010, 2012 Institute for Dutch Lexicology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
/**
 *
 */
package nl.inl.blacklab.index.complex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.inl.blacklab.index.complex.ComplexFieldProperty.SensitivitySetting;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;

/**
 * A complex field is like a Lucene field, but in addition to its "normal" value, it can have
 * multiple properties per word (not just a single token). The properties might be "headword", "pos"
 * (part of speech), "namedentity" (whether or not the word is (part of) a named entity like a
 * location or place), etc.
 *
 * Complex fields are implemented by indexing a field in Lucene for each property. For example, if
 * complex field "contents" has properties "headword" and "pos", there would be 3 Lucene fields for
 * the complex field: "contents", "contents__headword" and "contents__pos".
 *
 * The main field ("contents" in the above example) may include offset information if you want (e.g.
 * for highlighting). All Lucene fields will include position information (for use with
 * SpanQueries).
 *
 * N.B. It is crucial that everything stays in synch, so you should call all the appropriate add*()
 * methods for each property and each token, or use the correct position increments to keep everything
 * synched up. The same goes for addStartChar() and addEndChar() (although, if you don't want any
 * offsets, you need not call these).
 */
public class ComplexFieldImpl extends ComplexField {
	private Map<String, ComplexFieldProperty> properties = new HashMap<String, ComplexFieldProperty>();

	private List<Integer> start = new ArrayList<Integer>();

	private List<Integer> end = new ArrayList<Integer>();

	private String fieldName;

	private ComplexFieldProperty mainProperty;

	/** @param name
	 * @param filterAdder
	 * @deprecated use constructor with sensitivity parameter */
	@Deprecated
	public ComplexFieldImpl(String name, TokenFilterAdder filterAdder) {
		this(name, null, filterAdder, true);
	}

	/** @param name
	 * @param mainProperty
	 * @param filterAdder
	 * @deprecated use constructor with sensitivity parameter */
	@Deprecated
	public ComplexFieldImpl(String name, String mainProperty, TokenFilterAdder filterAdder) {
		this(name, mainProperty, filterAdder, true);
	}

	/** @param name
	 * @param filterAdder
	 * @param includeOffsets
	 * @deprecated use constructor with sensitivity parameter */
	@Deprecated
	public ComplexFieldImpl(String name, TokenFilterAdder filterAdder, boolean includeOffsets) {
		this(name, null, filterAdder, includeOffsets);
	}

	/** @param name
	 * @param mainPropertyName
	 * @param filterAdder
	 * @param includeOffsets
	 * @deprecated use constructor with sensitivity parameter */
	@Deprecated
	public ComplexFieldImpl(String name, String mainPropertyName, TokenFilterAdder filterAdder, boolean includeOffsets) {
		fieldName = name;
		if (mainPropertyName == null)
			mainPropertyName = ComplexFieldUtil.getDefaultMainPropName();
		mainProperty = new ComplexFieldPropertyImplLargeDoc(mainPropertyName, filterAdder, includeOffsets);
		properties.put(mainPropertyName, mainProperty);
	}

	/**
	 * Construct a ComplexField object with a main property
	 * @param name field name
	 * @param mainPropertyName main property name
	 * @param sensitivity ways to index main property, with respect to case- and
	 *   diacritics-sensitivity.
	 */
	public ComplexFieldImpl(String name, String mainPropertyName, SensitivitySetting sensitivity) {
		boolean includeOffsets = true;
		fieldName = name;
		if (mainPropertyName == null)
			mainPropertyName = ComplexFieldUtil.getDefaultMainPropName();
		mainProperty = new ComplexFieldPropertyImplLargeDoc(mainPropertyName, sensitivity, includeOffsets);
		properties.put(mainPropertyName, mainProperty);
	}

	@Override
	public int numberOfTokens() {
		return start.size();
	}

	@Deprecated
	@Override
	public void addProperty(String name, TokenFilterAdder filterAdder) {
		ComplexFieldProperty p = new ComplexFieldPropertyImplLargeDoc(name, filterAdder, false);
		properties.put(name, p);
	}

	@Override
	public ComplexFieldProperty addProperty(String name, SensitivitySetting sensitivity) {
		ComplexFieldProperty p = new ComplexFieldPropertyImplLargeDoc(name, sensitivity, false);
		properties.put(name, p);
		return p;
	}

	@Override
	public void addPropertyAlternative(String propName, String altName) {
		addPropertyAlternative(propName, altName, null);
	}

	@Override
	public void addPropertyAlternative(String propName, String altName, TokenFilterAdder filterAdder) {
		ComplexFieldProperty p = properties.get(propName);
		if (p == null)
			throw new RuntimeException("Undefined property '" + propName + "'");
		p.addAlternative(altName, filterAdder);
	}

	@Deprecated
	@Override
	public void addProperty(String name) {
		addProperty(name, (TokenFilterAdder)null);
	}

	@Override
	public void addStartChar(int startChar) {
		start.add(startChar);
	}

	@Override
	public void addEndChar(int endChar) {
		end.add(endChar);
	}

	@Override
	public void addValue(String value, int posIncr) {
		//ComplexFieldProperty p = properties.get(mainPropertyName);
		mainProperty.addValue(value, posIncr);
	}

	@Override
	public void addPropertyValue(String name, String value, int posIncr) {
		ComplexFieldProperty p = properties.get(name);
		if (p == null)
			throw new RuntimeException("Undefined property '" + name + "'");
		p.addValue(value, posIncr);
	}

	@Override
	public void addToLuceneDoc(Document doc) {
		for (ComplexFieldProperty p : properties.values()) {
			p.addToLuceneDoc(doc, fieldName, start, end);
		}

		// Add number of tokens in complex field as a stored field,
		// because we need to be able to find this property quickly
		// for SpanQueryNot.
		doc.add(new Field(ComplexFieldUtil.lengthTokensField(fieldName),
				"" + numberOfTokens(), Store.YES, Index.NOT_ANALYZED_NO_NORMS));
	}

	@Override
	public void clear() {
		start.clear();
		end.clear();
		for (ComplexFieldProperty p : properties.values()) {
			p.clear();
		}
	}

	@Override
	public void addAlternative(String altName) {
		mainProperty.addAlternative(altName, null);
	}

	@Override
	public void addAlternative(String altName, TokenFilterAdder filterAdder) {
		mainProperty.addAlternative(altName, filterAdder);
	}

	@Override
	public ComplexFieldProperty getProperty(String name) {
		ComplexFieldProperty p = properties.get(name);
		if (p == null)
			throw new RuntimeException("Undefined property '" + name + "'");
		return p;
	}

	@Override
	public List<String> getPropertyValues(String name) {
		ComplexFieldProperty p = properties.get(name);
		if (p == null)
			throw new RuntimeException("Undefined property '" + name + "'");
		return p.getValues();
	}

	@Override
	public List<Integer> getPropertyPositionIncrements(String name) {
		ComplexFieldProperty p = properties.get(name);
		if (p == null)
			throw new RuntimeException("Undefined property '" + name + "'");
		return p.getPositionIncrements();
	}

	@Override
	public ComplexFieldProperty getMainProperty() {
		return mainProperty;
	}

	@Override
	public String getName() {
		return fieldName;
	}

	@Override
	public Collection<ComplexFieldProperty> getProperties() {
		return properties.values();
	}

}
