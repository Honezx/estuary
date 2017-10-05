package org.mengpan.deeplearning.components.optimizer
import breeze.linalg.DenseMatrix

/**
  * Stochastic Gradient Descent, i.e. Mini-batch Gradient Descent.
  */
class SGDOptimizer extends Optimizer with MiniBatchable with NonHeuristic {

  /**
    * Implementation of Optimizer.optimize(). Optimizing Machine Learning-like models'
    * parameters on a training dataset (feature, label).
    * @param feature DenseMatrix of shape (n, p) where n: the number of
    *                training examples, p: the dimension of input feature.
    * @param label DenseMatrix of shape (n, q) where n: the number of
    *              training examples, q: number of distinct labels.
    * @param initParams Initialized parameters.
    * @param forwardFunc The cost function.
    *                    inputs: (feature, label, params) of type
    *                           (DenseMatrix[Double], DenseMatrix[Double], T)
    *                    output: cost of type Double.
    * @param backwardFunc A function calculating gradients of all parameters.
    *                     input: (label, params) of type (DenseMatrix[Double], T)
    *                     output: gradients of params of type T.
    * @tparam T The type of model parameters.
    *           For Neural Network, T is List[DenseMatrix[Double]]
    * @return Trained parameters.
    */
  override def optimize[T <: Seq[DenseMatrix[Double]]](feature: DenseMatrix[Double], label: DenseMatrix[Double])
                                                      (initParams: T)
                                                      (forwardFunc: (DenseMatrix[Double], DenseMatrix[Double], T) => Double)
                                                      (backwardFunc: (DenseMatrix[Double], T) => T): T = {
    val printMiniBatchUnit = ((feature.rows / this.miniBatchSize).toInt / 5).toInt //for each iteration, only print minibatch cost FIVE times.

    (0 until this.iteration).toIterator.foldLeft[T](initParams){case (preParams, iterTime) =>
      val minibatches = getMiniBatches(feature, label)
      minibatches.zipWithIndex.foldLeft[T](preParams){case (preBatchParams, ((batchFeature, batchLabel), miniBatchTime)) =>
        val cost = forwardFunc(batchFeature, batchLabel, preBatchParams)
        val grads = backwardFunc(batchLabel, preBatchParams)

        if (miniBatchTime % printMiniBatchUnit == 0)
          logger.info("Iteration: " + iterTime + "|=" + "=" * (miniBatchTime / 10) + ">> Cost: " + cost)
        costHistory.+=(cost)

        updateFunc(preBatchParams, grads)
      }
    }
  }

  /**
    * Update model parameters using Gradient Descent method.
    * @param params Model parameters' values on current iteration.
    * @param grads Gradients of model parameters on current iteration.
    * @tparam T the type of model parameters. For neural network, T is List[DenseMatrix[Double]]
    * @return Updated model parameters.
    */
  private def updateFunc[T <: Seq[DenseMatrix[Double]]](params: T, grads: T): T = {
    val res = for {(param, grad) <- params.zip(grads)} yield (param - learningRate * grad)
    res.asInstanceOf[T]
  }
}

object SGDOptimizer {
  def apply(miniBatchSize: Int): SGDOptimizer = {
    new SGDOptimizer()
      .setMiniBatchSize(miniBatchSize)
  }
}
