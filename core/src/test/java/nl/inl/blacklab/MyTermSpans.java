package nl.inl.blacklab;

/*
 * Copyright 2005 The Apache Software Foundation
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
 */


import java.io.IOException;
import java.util.Objects;

import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.spans.SpanCollector;
import org.apache.lucene.search.spans.Spans;

/**
 * Expert:
 * Public for extension only.
 * This does not work correctly for terms that indexed at position Integer.MAX_VALUE.
 *
 * Adapted from Lucene version: removed assert that interfered with testing using MockSpans.
 */
public class MyTermSpans extends Spans {
  protected final PostingsEnum postings;
  protected final Term term;
  protected int doc;
  protected int freq;
  protected int count;
  protected int position;
  protected boolean readPayload;
  private final float positionsCost;

  public MyTermSpans(PostingsEnum postings, Term term, float positionsCost) {
    super();
    this.postings = Objects.requireNonNull(postings);
    this.term = Objects.requireNonNull(term);
    this.doc = -1;
    this.position = -1;
    assert positionsCost > 0; // otherwise the TermSpans should not be created.
    this.positionsCost = positionsCost;
  }

  @Override
  public int nextDoc() throws IOException {
    doc = postings.nextDoc();
    if (doc != DocIdSetIterator.NO_MORE_DOCS) {
      freq = postings.freq();
      assert freq >= 1;
      count = 0;
    }
    position = -1;
    return doc;
  }

  @Override
  public int advance(int target) throws IOException {
    assert target > doc;
    doc = postings.advance(target);
    if (doc != DocIdSetIterator.NO_MORE_DOCS) {
      freq = postings.freq();
      assert freq >= 1;
      count = 0;
    }
    position = -1;
    return doc;
  }

  @Override
  public int docID() {
    return doc;
  }

  @Override
  public int nextStartPosition() throws IOException {
    if (count == freq) {
      assert position != NO_MORE_POSITIONS;
      return position = NO_MORE_POSITIONS;
    }
    //int prevPosition = position;
    position = postings.nextPosition();
    //assert position >= prevPosition : "prevPosition="+prevPosition+" > position="+position;
    assert position != NO_MORE_POSITIONS; // int endPosition not possible
    count++;
    readPayload = false;
    return position;
  }

  @Override
  public int startPosition() {
    return position;
  }

  @Override
  public int endPosition() {
    return (position == -1) ? -1
          : (position != NO_MORE_POSITIONS) ? position + 1
          : NO_MORE_POSITIONS;
  }

  @Override
  public int width() {
    return 0;
  }

  @Override
  public long cost() {
    return postings.cost();
  }

  @Override
  public void collect(SpanCollector collector) throws IOException {
    collector.collectLeaf(postings, position, term);
  }

  @Override
  public float positionsCost() {
    return positionsCost;
  }

  @Override
  public String toString() {
    return "spans(" + term.toString() + ")@" +
            (doc == -1 ? "START" : (doc == NO_MORE_DOCS) ? "ENDDOC"
              : doc + " - " + (position == NO_MORE_POSITIONS ? "ENDPOS" : position));
  }

  public PostingsEnum getPostings() {
    return postings;
  }
}
