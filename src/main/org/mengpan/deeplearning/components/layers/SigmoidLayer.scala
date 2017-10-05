package org.mengpan.deeplearning.components.layers
import breeze.linalg.DenseMatrix
import breeze.numerics.sigmoid
import org.mengpan.deeplearning.utils.MyDict

/**
  * Created by mengpan on 2017/8/26.
  */
class SigmoidLayer extends Layer{

  protected override def activationFuncEval(zCurrent: DenseMatrix[Double]): DenseMatrix[Double] = {
    sigmoid(zCurrent)
  }

  protected override def activationGradEval(zCurrent: DenseMatrix[Double]): DenseMatrix[Double] = {
    val sigmoided = sigmoid(zCurrent)
    sigmoided *:* (1.0 - sigmoided)
  }
}

object SigmoidLayer {
  def apply(numHiddenUnits: Int, batchNorm: Boolean = false): SigmoidLayer = {
    new SigmoidLayer()
      .setNumHiddenUnits(numHiddenUnits)
      .setBatchNorm(batchNorm)
  }
}

