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
package nl.inl.blacklab.search.results;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.inl.blacklab.resultproperty.ComparatorGroupProperty;
import nl.inl.blacklab.resultproperty.GroupProperty;
import nl.inl.blacklab.resultproperty.HitProperty;
import nl.inl.blacklab.resultproperty.PropertyValue;
import nl.inl.blacklab.search.indexmetadata.Annotation;

/**
 * Groups results on the basis of a list of criteria, and provide random access
 * to the resulting groups.
 *
 * This implementation doesn't care in what order the spans appear, it will just
 * retrieve all of them and put each of them in a group. This takes more memory
 * and time than if the spans to be grouped are sequential (in which case you
 * should use ResultsGrouperSequential).
 */
public class HitGroupsImpl extends HitGroups {
    /**
     * Don't use this; use Hits.groupedBy().
     * 
     * @param hits hits to group
     * @param criteria criteria to group by
     * @return grouped hits
     */
    static HitGroups fromHits(Hits hits, HitProperty criteria) {
        return new HitGroupsImpl(hits, criteria);
    }

    /**
     * The groups.
     */
    private Map<PropertyValue, HitGroup> groups = new HashMap<>();

    /**
     * The groups, in sorted order.
     */
    private List<HitGroup> groupsOrdered = new ArrayList<>();

    /**
     * Total number of hits.
     */
    private int totalHits = 0;

    /**
     * Size of the largest group.
     */
    private int largestGroupSize = 0;

    /**
     * Construct a ResultsGrouper object, by grouping the supplied hits.
     *
     * NOTE: this will be made package-private in a future release. Use
     * Hits.groupedBy(criteria) instead.
     *
     * @param hits the hits to group
     * @param criteria the criteria to group on
     */
    HitGroupsImpl(Hits hits, HitProperty criteria) {
        super(hits.queryInfo(), criteria);
        
        List<Annotation> requiredContext = criteria.needsContext();
        criteria = criteria.copyWith(hits, requiredContext == null ? null : new Contexts(hits, requiredContext, criteria.needsContextSize(hits.index())));
        
        //Thread currentThread = Thread.currentThread();
        Map<PropertyValue, List<Hit>> groupLists = new HashMap<>();
        for (Hit hit: hits) {
            PropertyValue identity = criteria.get(hit);
            List<Hit> group = groupLists.get(identity);
            if (group == null) {
                group = new ArrayList<>();
                groupLists.put(identity, group);
            }
            group.add(hit);
            if (group.size() > largestGroupSize)
                largestGroupSize = group.size();
            totalHits++;
        }
        for (Map.Entry<PropertyValue, List<Hit>> e : groupLists.entrySet()) {
            PropertyValue groupId = e.getKey();
            List<Hit> hitList = e.getValue();
            HitGroup group = new HitGroup(queryInfo(), groupId, hitList);
            groups.put(groupId, group);
            groupsOrdered.add(group);
        }
    }

    /**
     * Get the total number of hits
     *
     * @return the number of hits
     */
    @Override
    public int getTotalResults() {
        return totalHits;
    }

    /**
     * Get all groups as a list
     *
     * @return the list of groups
     */
    @Override
    public List<HitGroup> getGroups() {
        return Collections.unmodifiableList(groupsOrdered);
    }

    /**
     * Sort groups
     *
     * @param prop the property to sort on
     * @param sortReverse whether to sort in descending order
     */
    @Override
    public void sortGroups(GroupProperty prop, boolean sortReverse) {
        Comparator<Group<?>> comparator = new ComparatorGroupProperty(prop, sortReverse);
        groupsOrdered.sort(comparator);
    }

    /**
     * Return the size of the largest group
     *
     * @return size of the largest group
     */
    @Override
    public int getLargestGroupSize() {
        return largestGroupSize;
    }

    /**
     * Return the number of groups
     *
     * @return number of groups
     */
    @Override
    public int numberOfGroups() {
        return groups.size();
    }

    @Override
    public String toString() {
        return "ResultsGrouper with " + numberOfGroups() + " groups";
    }

    @Override
    public HitGroup get(int i) {
        return groupsOrdered.get(i);
    }

    @Override
    public HitGroup get(PropertyValue identity) {
        return groups.get(identity);
    }

    @Override
    public int size() {
        return groups.size();
    }

//    @Override
//    public void sort(GroupProperty<Hit> sortBy, boolean reverse) {
//        sortGroups(sortBy, reverse);
//    }

    @Override
    public <G extends Group<Hit>> void add(G obj) {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public Results<HitGroup> window(int first, int windowSize) {
        throw new UnsupportedOperationException();
    }

}