package estuary.components.layers

import breeze.linalg.DenseMatrix

trait ReluActivator extends Activator{
  protected def activationFuncEval(zCurrent: DenseMatrix[Double]): DenseMatrix[Double] = {
    val res = DenseMatrix.zeros[Double](zCurrent.rows, zCurrent.cols)
    for {i <- (0 until zCurrent.rows).par
         j <- (0 until zCurrent.cols).par
    } {
      res(i, j) = if (zCurrent(i, j) >= 0) zCurrent(i, j) else 0.0
    }
    res
  }

  protected def activationGradEval(zCurrent: DenseMatrix[Double]): DenseMatrix[Double] = {
    val res = DenseMatrix.zeros[Double](zCurrent.rows, zCurrent.cols)

    for {i <- (0 until zCurrent.rows).par
         j <- (0 until zCurrent.cols).par
    } {
      res(i, j) = if (zCurrent(i, j) >= 0) 1.0 else 0.0
    }
    res
  }
}
