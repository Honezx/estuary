package estuary.components.optimizer

import breeze.linalg.DenseMatrix
import estuary.components.optimizer.AdamOptimizer.AdamParam
import org.slf4j.{Logger, LoggerFactory}

/**
  * Gradient Descent optimizer.
  */
class GDOptimizer(val iteration: Int, val learningRate: Double, val paramSavePath: String) extends Optimizer with NonHeuristic {
  override protected val logger: Logger = LoggerFactory.getLogger(this.getClass)

  /**
    * Implementation of Optimizer.optimize().
    * Optimizing Machine Learning-like models' parameters on a training
    * dataset (feature, label).
    *
    * @param feature      DenseMatrix of shape (n, p) where n: the number of
    *                     training examples, p: the dimension of input feature.
    * @param label        DenseMatrix of shape (n, q) where n: the number of
    *                     training examples, q: number of distinct labels.
    * @param initParams   Initialized parameters.
    * @param forwardFunc  The cost function.
    *                     inputs: (feature, label, params) of type
    *                     (DenseMatrix[Double], DenseMatrix[Double], T)
    *                     output: cost of type Double.
    * @param backwardFunc A function calculating gradients of all parameters.
    *                     input: (label, params) of type (DenseMatrix[Double], T)
    *                     output: gradients of params of type T.
    * @return Trained parameters.
    */
  def optimize(feature: DenseMatrix[Double], label: DenseMatrix[Double])
                       (initParams: Seq[DenseMatrix[Double]])
                       (forwardFunc: (DenseMatrix[Double], DenseMatrix[Double], Seq[DenseMatrix[Double]]) => Double)
                       (backwardFunc: (DenseMatrix[Double], Seq[DenseMatrix[Double]]) => Seq[DenseMatrix[Double]]): Seq[DenseMatrix[Double]] = {
    (0 until this.iteration).foldLeft[Seq[DenseMatrix[Double]]](initParams) { case (preParams, iterTime) =>
      val cost = forwardFunc(feature, label, preParams)

      Optimizer.printCostInfo(cost, iterTime, logger)
      addCostHistory(cost)

      val grads = backwardFunc(label, preParams)

      if (iterTime == 0) {
        logger.info("Starting checking correctness of gradients...")
        Optimizer.checkForGradients(preParams, grads, forwardFunc(feature, label, _), verbose = false)
        logger.info("Gradient checking passed")
      }

      updateFunc(preParams, grads)
    }
  }

  protected def handleGradientExplosionException(params: Any, paramSavePath: String): Unit = {
    saveDenseMatricesToDisk(params.asInstanceOf[AdamParam].modelParam, paramSavePath)
  }

  private def updateFunc(params: Seq[DenseMatrix[Double]], grads: Seq[DenseMatrix[Double]]): Seq[DenseMatrix[Double]] = {
    val res = for {(param, grad) <- params.zip(grads)} yield param - grad * learningRate
    res.asInstanceOf[Seq[DenseMatrix[Double]]]
  }
}

object GDOptimizer {
  def apply(iteration: Int = 100, learningRate: Double = 0.001, paramSavePath: String = System.getProperty("user.dir")): GDOptimizer = {
    new GDOptimizer(iteration, learningRate, paramSavePath)
  }
}

