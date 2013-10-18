/*
 * Copyright 2013 Google Inc.
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

package com.google.jimfs.internal;

import static com.google.jimfs.path.Normalization.CASE_FOLD_UNICODE;
import static com.google.jimfs.path.Normalization.CASE_FOLD_ASCII;
import static com.google.jimfs.path.Normalization.NFC;
import static com.google.jimfs.path.Normalization.NFD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableSet;
import com.google.jimfs.path.Normalization;

import org.junit.Test;

import java.util.regex.Pattern;

/**
 * Tests for {@link PathNormalizer}.
 *
 * @author Colin Decker
 */
public class PathNormalizerTest {

  private PathNormalizer normalizer;

  @Test
  public void testNone() {
    normalizer = PathNormalizer.none();

    assertNormalizedEqual("foo", "foo");
    assertNormalizedUnequal("Foo", "foo");
    assertNormalizedUnequal("\u00c5", "\u212b");
    assertNormalizedUnequal("Am\u00e9lie", "Ame\u0301lie");
  }

  private static final String[][] CASE_FOLD_TEST_DATA = {
      {"foo", "fOo", "foO", "Foo", "FOO"},
      {"eﬃcient", "efficient", "eﬃcient", "Eﬃcient", "EFFICIENT"},
      {"ﬂour", "flour", "ﬂour", "Flour", "FLOUR"},
      {"poſt", "post", "poſt", "Poſt", "POST"},
      {"poﬅ", "post", "poﬅ", "Poﬅ", "POST"},
      {"ﬅop", "stop", "ﬅop", "Stop", "STOP"},
      {"tschüß", "tschüss", "tschüß", "Tschüß", "TSCHÜSS"},
      {"weiß", "weiss", "weiß", "Weiß", "WEISS"},
      {"WEIẞ", "weiss", "weiß", "Weiß", "WEIẞ"},
      {"στιγμας", "στιγμασ", "στιγμας", "Στιγμας", "ΣΤΙΓΜΑΣ"},
      {"ᾲ στο διάολο", "ὰι στο διάολο", "ᾲ στο διάολο", "Ὰͅ Στο Διάολο", "ᾺΙ ΣΤΟ ΔΙΆΟΛΟ"},
      {"Henry Ⅷ", "henry ⅷ", "henry ⅷ", "Henry Ⅷ", "HENRY Ⅷ"},
      {"I Work At Ⓚ", "i work at ⓚ", "i work at ⓚ", "I Work At Ⓚ", "I WORK AT Ⓚ"},
      {"ʀᴀʀᴇ", "ʀᴀʀᴇ", "ʀᴀʀᴇ", "Ʀᴀʀᴇ", "ƦᴀƦᴇ"},
      {"Ὰͅ", "ὰι", "ᾲ", "Ὰͅ", "ᾺΙ"}
  };

  @Test
  public void testCaseFold() {
    normalizer = PathNormalizer.create(ImmutableSet.of(CASE_FOLD_UNICODE));

    for (String[] row : CASE_FOLD_TEST_DATA) {
      for (int i = 0; i < row.length; i++) {
        for (int j = i; j < row.length; j++) {
          assertNormalizedEqual(row[i], row[j]);
        }
      }
    }
  }

  @Test
  public void testCaseInsensitiveAscii() {
    normalizer = PathNormalizer.create(ImmutableSet.of(CASE_FOLD_ASCII));

    String[] row = {"foo", "FOO", "fOo", "Foo"};
    for (int i = 0; i < row.length; i++) {
      for (int j = i; j < row.length; j++) {
        assertNormalizedEqual(row[i], row[j]);
      }
    }

    assertNormalizedUnequal("weiß", "weiss");
  }

  private static final String[][] NORMALIZE_TEST_DATA = {
      {"\u00c5", "\u212b"}, // two forms of Å (one code point each)
      {"Am\u00e9lie", "Ame\u0301lie"} // two forms of Amélie (one composed, one decomposed)
  };

  @Test
  public void testNormalizeNfc() {
    normalizer = PathNormalizer.create(ImmutableSet.of(NFC));

    for (String[] row : NORMALIZE_TEST_DATA) {
      for (int i = 0; i < row.length; i++) {
        for (int j = i; j < row.length; j++) {
          assertNormalizedEqual(row[i], row[j]);
        }
      }
    }
  }

  @Test
  public void testNormalizeNfd() {
    normalizer = PathNormalizer.create(ImmutableSet.of(NFD));

    for (String[] row : NORMALIZE_TEST_DATA) {
      for (int i = 0; i < row.length; i++) {
        for (int j = i; j < row.length; j++) {
          assertNormalizedEqual(row[i], row[j]);
        }
      }
    }
  }

  private static final String[][] NORMALIZE_CASE_FOLD_TEST_DATA = {
      {"\u00c5", "\u00e5", "\u212b"},
      {"Am\u00e9lie", "Am\u00c9lie", "Ame\u0301lie", "AME\u0301LIE"}
  };

  @Test
  public void testNormalizeNfcCaseFold() {
    normalizer = PathNormalizer.create(ImmutableSet.of(NFC, CASE_FOLD_UNICODE));

    for (String[] row : NORMALIZE_CASE_FOLD_TEST_DATA) {
      for (int i = 0; i < row.length; i++) {
        for (int j = i; j < row.length; j++) {
          assertNormalizedEqual(row[i], row[j]);
        }
      }
    }
  }

  @Test
  public void testNormalizeNfdCaseFold() {
    normalizer = PathNormalizer.create(ImmutableSet.of(NFD, CASE_FOLD_UNICODE));

    for (String[] row : NORMALIZE_CASE_FOLD_TEST_DATA) {
      for (int i = 0; i < row.length; i++) {
        for (int j = i; j < row.length; j++) {
          assertNormalizedEqual(row[i], row[j]);
        }
      }
    }
  }

  private static final String[][] NORMALIZED_CASE_INSENSITIVE_ASCII_TEST_DATA = {
      {"\u00e5", "\u212b"},
      {"Am\u00e9lie", "AME\u0301LIE"}
  };

  @Test
  public void testNormalizeNfcCaseFoldAscii() {
    normalizer = PathNormalizer.create(ImmutableSet.of(NFC, CASE_FOLD_ASCII));

    for (String[] row : NORMALIZED_CASE_INSENSITIVE_ASCII_TEST_DATA) {
      for (int i = 0; i < row.length; i++) {
        for (int j = i + 1; j < row.length; j++) {
          assertNormalizedUnequal(row[i], row[j]);
        }
      }
    }
  }

  @Test
  public void testNormalizeNfdCaseFoldAscii() {
    normalizer = PathNormalizer.create(ImmutableSet.of(NFD, CASE_FOLD_ASCII));

    for (String[] row : NORMALIZED_CASE_INSENSITIVE_ASCII_TEST_DATA) {
      for (int i = 0; i < row.length; i++) {
        for (int j = i + 1; j < row.length; j++) {
          // since decomposition happens before case folding, the strings are equal when the
          // decomposed ASCII letter is folded
          assertNormalizedEqual(row[i], row[j]);
        }
      }
    }
  }

  // regex patterns offer loosely similar matching, but that's all

  @Test
  public void testNone_pattern() {
    normalizer = PathNormalizer.create(ImmutableSet.<Normalization>of());
    assertNormalizedPatternMatches("foo", "foo");
    assertNormalizedPatternDoesNotMatch("foo", "FOO");
    assertNormalizedPatternDoesNotMatch("FOO", "foo");
  }

  @Test
  public void testCaseFold_pattern() {
    normalizer = PathNormalizer.create(ImmutableSet.of(CASE_FOLD_UNICODE));
    assertNormalizedPatternMatches("foo", "foo");
    assertNormalizedPatternMatches("foo", "FOO");
    assertNormalizedPatternMatches("FOO", "foo");
    assertNormalizedPatternMatches("Am\u00e9lie", "AM\u00c9LIE");
    assertNormalizedPatternMatches("Ame\u0301lie", "AME\u0301LIE");
    assertNormalizedPatternDoesNotMatch("Am\u00e9lie", "Ame\u0301lie");
    assertNormalizedPatternDoesNotMatch("AM\u00c9LIE", "AME\u0301LIE");
    assertNormalizedPatternDoesNotMatch("Am\u00e9lie", "AME\u0301LIE");
  }

  @Test
  public void testCaseFoldAscii_pattern() {
    normalizer = PathNormalizer.create(ImmutableSet.of(CASE_FOLD_ASCII));
    assertNormalizedPatternMatches("foo", "foo");
    assertNormalizedPatternMatches("foo", "FOO");
    assertNormalizedPatternMatches("FOO", "foo");
    assertNormalizedPatternMatches("Ame\u0301lie", "AME\u0301LIE");
    assertNormalizedPatternDoesNotMatch("Am\u00e9lie", "AM\u00c9LIE");
    assertNormalizedPatternDoesNotMatch("Am\u00e9lie", "Ame\u0301lie");
    assertNormalizedPatternDoesNotMatch("AM\u00c9LIE", "AME\u0301LIE");
    assertNormalizedPatternDoesNotMatch("Am\u00e9lie", "AME\u0301LIE");
  }

  @Test
  public void testNormalizeNfc_pattern() {
    normalizer = PathNormalizer.create(ImmutableSet.of(NFC));
    assertNormalizedPatternMatches("foo", "foo");
    assertNormalizedPatternDoesNotMatch("foo", "FOO");
    assertNormalizedPatternDoesNotMatch("FOO", "foo");
    assertNormalizedPatternMatches("Am\u00e9lie", "Ame\u0301lie");
    assertNormalizedPatternDoesNotMatch("Am\u00e9lie", "AME\u0301LIE");
  }

  @Test
  public void testNormalizeNfd_pattern() {
    normalizer = PathNormalizer.create(ImmutableSet.of(NFD));
    assertNormalizedPatternMatches("foo", "foo");
    assertNormalizedPatternDoesNotMatch("foo", "FOO");
    assertNormalizedPatternDoesNotMatch("FOO", "foo");
    assertNormalizedPatternMatches("Am\u00e9lie", "Ame\u0301lie");
    assertNormalizedPatternDoesNotMatch("Am\u00e9lie", "AME\u0301LIE");
  }

  @Test
  public void testNormalizeNfcCaseFold_pattern() {
    normalizer = PathNormalizer.create(ImmutableSet.of(NFC, CASE_FOLD_UNICODE));
    assertNormalizedPatternMatches("foo", "foo");
    assertNormalizedPatternMatches("foo", "FOO");
    assertNormalizedPatternMatches("FOO", "foo");
    assertNormalizedPatternMatches("Am\u00e9lie", "AM\u00c9LIE");
    assertNormalizedPatternMatches("Ame\u0301lie", "AME\u0301LIE");
    assertNormalizedPatternMatches("Am\u00e9lie", "Ame\u0301lie");
    assertNormalizedPatternMatches("AM\u00c9LIE", "AME\u0301LIE");
    assertNormalizedPatternMatches("Am\u00e9lie", "AME\u0301LIE");
  }

  @Test
  public void testNormalizeNfdCaseFold_pattern() {
    normalizer = PathNormalizer.create(ImmutableSet.of(NFD, CASE_FOLD_UNICODE));
    assertNormalizedPatternMatches("foo", "foo");
    assertNormalizedPatternMatches("foo", "FOO");
    assertNormalizedPatternMatches("FOO", "foo");
    assertNormalizedPatternMatches("Am\u00e9lie", "AM\u00c9LIE");
    assertNormalizedPatternMatches("Ame\u0301lie", "AME\u0301LIE");
    assertNormalizedPatternMatches("Am\u00e9lie", "Ame\u0301lie");
    assertNormalizedPatternMatches("AM\u00c9LIE", "AME\u0301LIE");
    assertNormalizedPatternMatches("Am\u00e9lie", "AME\u0301LIE");
  }

  @Test
  public void testNormalizeNfcCaseFoldAscii_pattern() {
    normalizer = PathNormalizer.create(ImmutableSet.of(NFC, CASE_FOLD_ASCII));
    assertNormalizedPatternMatches("foo", "foo");
    assertNormalizedPatternMatches("foo", "FOO");
    assertNormalizedPatternMatches("FOO", "foo");

    // these are all a bit fuzzy as when CASE_INSENSITIVE is present but not UNICODE_CASE, ASCII
    // only strings are expected
    assertNormalizedPatternMatches("Ame\u0301lie", "AME\u0301LIE");
    assertNormalizedPatternDoesNotMatch("Am\u00e9lie", "AM\u00c9LIE");
    assertNormalizedPatternMatches("Am\u00e9lie", "Ame\u0301lie");
    assertNormalizedPatternMatches("AM\u00c9LIE", "AME\u0301LIE");
  }

  @Test
  public void testNormalizeNfdCaseFoldAscii_pattern() {
    normalizer = PathNormalizer.create(ImmutableSet.of(NFD, CASE_FOLD_ASCII));
    assertNormalizedPatternMatches("foo", "foo");
    assertNormalizedPatternMatches("foo", "FOO");
    assertNormalizedPatternMatches("FOO", "foo");

    // these are all a bit fuzzy as when CASE_INSENSITIVE is present but not UNICODE_CASE, ASCII
    // only strings are expected
    assertNormalizedPatternMatches("Ame\u0301lie", "AME\u0301LIE");
    assertNormalizedPatternDoesNotMatch("Am\u00e9lie", "AM\u00c9LIE");
    assertNormalizedPatternMatches("Am\u00e9lie", "Ame\u0301lie");
    assertNormalizedPatternMatches("AM\u00c9LIE", "AME\u0301LIE");
  }

  /**
   * Asserts that the given strings normalize to the same string using the current normalizer.
   */
  private void assertNormalizedEqual(String first, String second) {
    assertEquals(normalizer.normalize(first), normalizer.normalize(second));
  }

  /**
   * Asserts that the given strings normalize to different strings using the current normalizer.
   */
  private void assertNormalizedUnequal(String first, String second) {
    assertNotEquals(normalizer.normalize(first), normalizer.normalize(second));
  }

  /**
   * Asserts that the given strings match when one is compiled as a regex pattern using the current
   * normalizer and matched against the other.
   */
  private void assertNormalizedPatternMatches(String first, String second) {
    Pattern pattern = normalizer.compilePattern(first);
    assertTrue("pattern '" + pattern + "' does not match '" + second + "'",
        pattern.matcher(second).matches());

    pattern = normalizer.compilePattern(second);
    assertTrue("pattern '" + pattern + "' does not match '" + first + "'",
        pattern.matcher(first).matches());
  }

  /**
   * Asserts that the given strings do not match when one is compiled as a regex pattern using the
   * current normalizer and matched against the other.
   */
  private void assertNormalizedPatternDoesNotMatch(String first, String second) {
    Pattern pattern = normalizer.compilePattern(first);
    assertFalse("pattern '" + pattern + "' should not match '" + second + "'",
        pattern.matcher(second).matches());

    pattern = normalizer.compilePattern(second);
    assertFalse("pattern '" + pattern + "' should not match '" + first + "'",
        pattern.matcher(first).matches());
  }
}
