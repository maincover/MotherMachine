/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cellst.Enums;

/**
 * All steps in images treatments.
 * 
 * @author Magali Vangkeosay, David Parsons
 */
public enum Step
{
    ORIG,
    ROUGH_BACKGROUND,
    DENOIS,
    FINAL_BACKGROUND,
    RECENTER,
    RENORM,
    SEEDS,
    BLOBS,
    FINAL_BLOBS,
    BLOBSOLVER
}
