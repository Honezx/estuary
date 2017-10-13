package estuary.components.layers

import breeze.linalg.{DenseMatrix, DenseVector}
import estuary.components.regularizer.Regularizer

/**
  * Created by mengpan on 2017/9/7.
  */
class DropoutLayer extends Layer {

  protected var dropoutRate: Double = _

  override def setPreviousHiddenUnits(numHiddenUnits: Int): this.type = {
    super.setPreviousHiddenUnits(numHiddenUnits)
    this.numHiddenUnits = previousHiddenUnits
    this
  }

  def setDropoutRate(dropoutRate: Double): this.type = {
    assert(dropoutRate <= 1 && dropoutRate >= 0, "dropout rate must be between 0 and 1")

    this.dropoutRate = dropoutRate
    this
  }

  protected var dropoutVector: DenseVector[Double] = _

  protected def activationFuncEval(zCurrent: DenseMatrix[Double]): DenseMatrix[Double] = {
    dropoutVector = generateDropoutVector(numHiddenUnits, dropoutRate)

    val numExamples = zCurrent.rows
    val oneVector = DenseVector.ones[Double](numExamples)
    zCurrent *:* (oneVector * dropoutVector.t) / (1.0 - this.dropoutRate)
  }

  protected def activationGradEval(zCurrent: DenseMatrix[Double]): DenseMatrix[Double] = {
    val numExamples = zCurrent.rows
    val oneVector = DenseVector.ones[Double](numExamples)

    (oneVector * dropoutVector.t) / (1.0 - this.dropoutRate)
  }

  private def generateDropoutVector(numHiddenUnits: Int, dropoutRate: Double): DenseVector[Double] = {
    val randVec = DenseVector.rand[Double](numHiddenUnits)

    val res = DenseVector.zeros[Double](randVec.length)
    for (i <- (0 until randVec.length).par) {
      res(i) = if (randVec(i) <= this.dropoutRate) 0.0 else 1.0
    }
    res
  }


  override def forward(yPrevious: DenseMatrix[Double]): DenseMatrix[Double] = {
    this.yPrevious = yPrevious
    activationFuncEval(yPrevious)
  }

  override def backward(dYCurrent: DenseMatrix[Double], regularizer: Option[Regularizer]): (DenseMatrix[Double], DenseMatrix[Double]) = {
    val filterMat = activationGradEval(yPrevious)

    (dYCurrent *:* filterMat, DenseMatrix.zeros[Double](previousHiddenUnits + 1, numHiddenUnits))
  }

  def copyStructure: DropoutLayer = {
    new DropoutLayer()
      .setDropoutRate(dropoutRate)
      .setBatchNorm(batchNorm)
      .setNumHiddenUnits(numHiddenUnits)
      .setPreviousHiddenUnits(previousHiddenUnits)
  }
}

object DropoutLayer {
  def apply(dropoutRate: Double): DropoutLayer = {
    new DropoutLayer()
      .setNumHiddenUnits(100)
      .setDropoutRate(dropoutRate)
      .setBatchNorm(false)
  }
}
