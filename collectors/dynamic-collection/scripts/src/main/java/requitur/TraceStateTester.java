package requitur;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import requitur.content.Content;
import requitur.content.RuleContent;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TraceStateTester {

   private static final Logger LOG = LoggerFactory.getLogger(TraceStateTester.class);

   public static void assureCorrectState(final Sequitur sequitur) {
      testTrace(sequitur);
      testDigrams(sequitur);
      testRules(sequitur);
      testRuleUsage(sequitur);
   }

   private static void testRuleUsage(final Sequitur sequitur) {
      List<Content> uncompressedTrace = sequitur.getUncompressedTrace();

      for (final Content traceElement : uncompressedTrace) {
         if (traceElement instanceof RuleContent && !sequitur.rules.containsKey(((RuleContent) traceElement).getValue())) {
            throw new RuntimeException("Rule missing!");
         }
      }

      for (final Rule r : sequitur.getRules().values()) {
         if (r.getElements().size() == 2) {

            final Digram keyDigram = new Digram(
                new Symbol(sequitur, r.getElements().get(0).getValue()),
                new Symbol(sequitur, r.getElements().get(1).getValue())
            );

            final Digram test = sequitur.digrams.get(keyDigram);

            if (test == null) {
               LOG.error(String.valueOf(keyDigram));
            }

            else if (test.rule == null || test.rule != r) {
               throw new RuntimeException(r.getElements() + " should have  rule " + r + " but has " + test.rule);
            }
         }
      }
   }

   public static void testTrace(final Sequitur sequitur) {
      List<Content> uncompressedTrace = sequitur.getUncompressedTrace();
      Map<String, Rule> rules = sequitur.getRules();

      LOG.debug(uncompressedTrace.toString());
      LOG.debug(rules.toString());
      final List<Content> expandedTrace = expandContentTrace(uncompressedTrace, rules);
//      final List<Content> fullTrace = sequitur.addingElements.subList(0, expandedTrace.size());
//      assertEquals(fullTrace, expandedTrace);
   }

   private static void testRules(final Sequitur sequitur) {
      for (final Rule r : sequitur.rules.values()) {
         // System.out.println(r);
         int usages = 0;
         List<Content> uncompressedTrace = sequitur.getUncompressedTrace();

         for (final Content traceElement : uncompressedTrace) {
            if (traceElement instanceof RuleContent && (((RuleContent) traceElement).getValue().equals(r.getName()))) {
               usages++;
            }
         }

         for (final Rule other : sequitur.rules.values()) {
            for (final ReducedTraceElement otherElement : other.getElements()) {
               if (otherElement.getValue() instanceof RuleContent && ((RuleContent) otherElement.getValue()).getValue().equals(r.getName())) {
                  usages++;
               }
            }
         }

         if (usages < 2) {
            throw new RuntimeException("Rule " + r.getName() + " underused: " + usages);
         }
      }
   }

   private static void testDigrams(final Sequitur sequitur) {
      final Set<Digram> currentDigrams = new HashSet<>(sequitur.digrams.values());
      Content before = null;

      List<Content> uncompressedTrace = sequitur.getUncompressedTrace();

      for (final Content trace : uncompressedTrace) {
         if (before != null) {
            final Digram di = new Digram(
                new Symbol(sequitur, before),
                new Symbol(sequitur, trace)
            );

            currentDigrams.remove(di);
         }

         before = trace;
      }

      for (final Rule r : sequitur.rules.values()) {
         before = null;
         if (r.getElements().size() == 1) {
            throw new RuntimeException("Rule consists of only one symbol: " + r);
         }

         for (final ReducedTraceElement trace : r.getElements()) {
            if (before != null) {
               final Digram di = new Digram(
                   new Symbol(sequitur, before),
                   new Symbol(sequitur, trace.getValue())
               );

               currentDigrams.remove(di);
            }

            before = trace.getValue();
         }
      }

      if (currentDigrams.size() > 0) {
         System.out.println(sequitur.getUncompressedTrace());
         System.out.println(sequitur.rules);
         throw new RuntimeException("Digram not existing but listed: " + currentDigrams);
      }
      
      final List<Content> trace = sequitur.getTrace();

      for (int index = 1; index < trace.size(); index++) {
         final Content predecessor = trace.get(index - 1);
         final Content current = trace.get(index);
         final Digram digram = new Digram(
             new Symbol(sequitur, predecessor),
             new Symbol(sequitur, current)
         );

         final Digram other = sequitur.digrams.get(digram);
         System.out.println("Search: " + digram);

         if (other == null) {
            throw new RuntimeException("Digram exists but is not listed: " + digram);
         }
      }

      // This is the most performance-destroying implementation one could think of - only for testing functionality, not for production!
      for (int index = 1; index < trace.size(); index++) {
         final Content predecessor = trace.get(index - 1);
         final Content current = trace.get(index);

         for (int indexCompare = index + 2; indexCompare < trace.size(); indexCompare++) {
            final Content comparePredecessor = trace.get(indexCompare-1);
            final Content compareCurrent = trace.get(indexCompare);

            if (compareCurrent.equals(current) && comparePredecessor.equals(predecessor)) {
               throw new RuntimeException("Digram " + predecessor + " " + current + " appears twice in trace!");
            }
         }
      }
   }

   public static List<Content> expandReadableTrace(final List<ReducedTraceElement> trace, final Map<String, Rule> rules) {
      final List<Content> result = new ArrayList<>();

      for (final ReducedTraceElement element : trace) {

         for (int i = 0; i < element.getOccurrences(); i++) {
            if (element.getValue() instanceof RuleContent) {
               final String value = ((RuleContent) element.getValue()).getValue();
               final Rule rule = rules.get(value);
               result.addAll(expandReadableTrace(rule.getElements(), rules));

            } else {
               result.add(element.getValue());
            }
         }
      }

      return result;
   }

   public static List<Content> expandContentTrace(final List<Content> trace, final Map<String, Rule> rules) {
      final List<Content> result = new ArrayList<>();

      for (final Content element : trace) {
         if (element instanceof RuleContent) {
            final String value = ((RuleContent) element).getValue();
            final Rule rule = rules.get(value);
            result.addAll(expandTrace(rule.getElements(), rules));
         } else {
            result.add(element);
         }
      }

      return result;
   }

   public static List<Content> expandTrace(final List<ReducedTraceElement> trace, final Map<String, Rule> rules) {
      final List<Content> result = new ArrayList<>();

      for (final ReducedTraceElement element : trace) {
         for (int i = 0; i < element.getOccurrences(); i++) {

            if (element.getValue() instanceof RuleContent) {
               final String value = ((RuleContent) element.getValue()).getValue();
               final Rule rule = rules.get(value);
//               System.out.println("Expanding: " + value);
               final List<Content> expandedElements = expandTrace(rule.getElements(), rules);
               result.addAll(expandedElements);

            } else {
               result.add(element.getValue());
            }
         }
      }
      return result;
   }
}
