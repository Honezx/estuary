package estuary.components.optimizer

import java.text.SimpleDateFormat
import java.util.Calendar

import breeze.linalg.DenseMatrix
import breeze.numerics.abs
import estuary.components.Exception.GradientExplosionException
import estuary.model.Model
import org.slf4j.Logger

import scala.collection.mutable.ArrayBuffer


/**
  * Optimizer Interface, all of whose implementations MUST implement abstract method: "optimize"
  *
  * @note This optimizer can be ONLY used for optimizing Machine Learning-like
  *       problems, which means that input-output (i.e. feature-label) data are needed.
  *       NOT for general optimization or mathematical planning problem.
  */
trait Optimizer extends Serializable{
  protected val logger: Logger

  protected val iteration: Int
  protected val learningRate: Double
  protected val paramSavePath: String

  /**
    * Optimizing Machine Learning-like models' parameters on a training data set (feature, label).
    *
    * @param feature      feature matrix
    * @param label        label matrix in one-hot representation
    * @param initParams   Initialized parameters.
    * @param forwardFunc  The cost function.
    *                     inputs: (feature, label, params) of type (DenseMatrix[Double], DenseMatrix[Double], T)
    *                     output: cost of type Double.
    * @param backwardFunc A function calculating gradients of all parameters.
    *                     input: (label, params) of type (DenseMatrix[Double], T)
    *                     output: gradients of params of type T.
    * @return Trained parameters.
    */
  def optimize(feature: DenseMatrix[Double], label: DenseMatrix[Double])
              (initParams: Seq[DenseMatrix[Double]])
              (forwardFunc: (DenseMatrix[Double], DenseMatrix[Double], Seq[DenseMatrix[Double]]) => Double)
              (backwardFunc: (DenseMatrix[Double], Seq[DenseMatrix[Double]]) => Seq[DenseMatrix[Double]]): Seq[DenseMatrix[Double]]

  protected def handleGradientExplosionException(params: Any, paramSavePath: String): Unit

  protected def addCostHistory(cost: Double): Unit = {
    costHistory.+=(cost)
    minCost = if (cost < minCost) cost else minCost
  }

  protected var exceptionCount: Int = 0
  protected var minCost: Double = 100

  /** Storing cost history after every iteration. */
  val costHistory: ArrayBuffer[Double] = new ArrayBuffer[Double]()

  @throws[GradientExplosionException]
  protected def checkGradientsExplosion(nowCost: Double, minCost: Double): Unit = {
    if (nowCost - minCost > 10)
      throw new GradientExplosionException(s"Cost is rising ($minCost -> $nowCost), it seems that gradient explosion happens...")
  }

  protected def saveDenseMatricesToDisk(params: Seq[DenseMatrix[_]], paramSavePath: String): Unit = {
    val modelParams = params
    val currentTime = Calendar.getInstance().getTime
    val timeFormat = new SimpleDateFormat("yyyyMMddHHmmss")
    val fileName = paramSavePath + "/" + timeFormat.format(currentTime) + ".txt"
    Model.saveDenseMatricesToDisk(modelParams, fileName)
    logger.warn(s"Something wrong happened during training, the current parameters have been save to $fileName")
  }
}

object Optimizer {
  def printCostInfo(cost: Double, iterTime: Int, logger: Logger): Unit = {
    logger.info("Iteration: " + iterTime + "| Cost: " + cost)
  }

  def checkForGradients(param: Seq[DenseMatrix[Double]],
                        grads: Seq[DenseMatrix[Double]],
                        func: Seq[DenseMatrix[Double]] => Double,
                        epsilon: Double = 1e-10,
                        precision: Double = 1e-3,
                        verbose: Boolean = true): Unit = {
    for {n <- param.indices
         i <- 0 until param(n).rows
         j <- 0 until param(n).cols
    } {
      param(n)(i, j) += epsilon
      val cost1 = func(param)
      param(n)(i, j) -= 2.0 * epsilon
      val cost2 = func(param)
      val grad = (cost1 - cost2) / (2.0 * epsilon)
      assert(abs(grad - grads(n)(i, j)) < precision, s"Correct gradient for ${n}th param in ${i}th row and ${j}th col is: $grad, while your gradient is: ${grads(n)(i, j)}")
      if (verbose) {
        println(s"Numeric gradient for ${n}th param in ${i}th row and ${j}th col is: $grad")
        println(s"Your gradient for ${n}th param in ${i}th row and ${j}th col is: ${grads(n)(i, j)}")
      }
    }
  }
}
