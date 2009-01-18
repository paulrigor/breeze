package scalanlp.stats;

import scala.collection.mutable._;
import scalanlp.data._;
import scalanlp.classify._;

/** Implements statistical significance testing for the output of two systems by randomization. 
* This system assumes they're on the same dataset, which changes the procedure.
* Follows Teh, 2000 More accurate tests for the statistical significance of result differences.
*
* Labels must have equals.
*
* @author dlwh
*/
// TODO: use quasi-random bit sequence.
class RandomizationTest[L](val numSamples:Int, val errorMeasure: Seq[L]=>Double) extends ((Seq[L],Seq[L])=>Double) {
  def this(errorMeasure: Seq[L]=>Double) = this(5000,errorMeasure);

  def diff(l1: Seq[L], l2: Seq[L]) = Math.abs( errorMeasure(l1) - errorMeasure(l2));
  def apply(labeling1: Seq[L], labeling2: Seq[L]) = {
    assume(labeling1.length == labeling2.length);
    // git rid of any overlapping labels
    val lpairs = (labeling1.elements zip labeling2.elements).filter( a => a._1 != a._2).collect;
    val baseDiff = diff(lpairs.projection.map(_._1),lpairs.projection.map(_._2));
    var numBetter = 0;
    for(i <- 1 to numSamples) {
      val l1 = new ArrayBuffer[L]();
      val l2 = new ArrayBuffer[L]();
      for( (a,b) <- lpairs) {
        if(Rand.uniform.get < .5) {
          l1 += a;
          l2 += b;
        } else {
          l1 += b;
          l2 += a;
        }
      }
      if(baseDiff < diff(l1,l2)) {
        numBetter += 1;
      }
    }
    (numBetter + 1.0) / (numSamples+1.0);
  }
}

object RandomizationTest {

  /** Classify the dataset according to the two classifiers, and then use some
  * measure from ContingencyStats to see if they're different.
  */
  def apply[L,T](dataset: Seq[Example[L,T]], c1: Classifier[L,T], c2: Classifier[L,T], error: ContingencyStats[L]=>Double):Double = {
    new RandomizationTest[L]( l1=> 
      error(ContingencyStats(l1,dataset.map(_.label)))
    ) apply (dataset.projection.map(c1).force,dataset.projection.map(c2).force);
  }

  /** Classify the dataset according to the two classifiers, and then use f1
  * measure from ContingencyStats to see if they're different.
  */
  def apply[L,T](dataset: Seq[Example[L,T]], c1: Classifier[L,T], c2: Classifier[L,T]):Double = {
    apply(dataset,c1,c2,(x:ContingencyStats[L]) => x.macroaveraged.f);
  }

}