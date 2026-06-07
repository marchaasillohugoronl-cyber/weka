package com.streamflix.servicio;

import weka.classifiers.Evaluation;
import weka.classifiers.trees.RandomForest;
import weka.core.*;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Modelo de recomendación basado en RandomForest (Weka).
 *
 * RandomForest construye N árboles sobre submuestras aleatorias del dataset
 * y promedia sus distribuciones de probabilidad. Esto produce confianzas
 * mucho más calibradas que un único árbol J48 (evita valores extremos 0%/100%).
 *
 * Atributos del dataset (orden ARFF):
 *   [0] edad              → numeric
 *   [1] categoria         → nominal {ACCION,ROMANCE,TERROR,COMEDIA,CIENCIA_FICCION}
 *   [2] puntuacion        → numeric
 *   [3] esFavorito        → nominal {SI,NO}
 *   [4] numCalificaciones → numeric  (historial del usuario en ese género)
 *   [5] recomendar        → clase {SI,NO}
 */
@Service
public class WekaServicio {

    private static final int NUM_ARBOLES = 100;

    private RandomForest modelo;
    private Instances estructura;

    public void entrenar(Instances data) throws Exception {
        this.estructura = data;
        estructura.setClassIndex(estructura.numAttributes() - 1);

        modelo = new RandomForest();
        modelo.setNumIterations(NUM_ARBOLES);
        modelo.setNumFeatures(0);   // 0 = sqrt(numAtributos), valor óptimo por defecto
        modelo.setMaxDepth(0);      // sin límite de profundidad
        modelo.buildClassifier(estructura);

        System.out.println("=== RandomForest entrenado ===");
        System.out.printf("Árboles: %d | Instancias: %d | Atributos: %d%n",
                NUM_ARBOLES, (int) data.numInstances(), data.numAttributes() - 1);
    }

    /**
     * Predice si una película debe recomendarse.
     *
     * @param edad              edad del usuario
     * @param categoria         categoría normalizada de la película
     * @param puntuacion        promedio histórico del usuario en ese género (fallback 3.0)
     * @param esFavorito        "SI" si el género está en géneros favoritos
     * @param numCalificaciones cuántas películas de ese género ha calificado el usuario
     * @return "SI" o "NO"
     */
    public String predecir(double edad, String categoria, double puntuacion,
                           String esFavorito, double numCalificaciones) throws Exception {

        if (modelo == null || estructura == null) {
            throw new IllegalStateException("El modelo no ha sido entrenado.");
        }

        DenseInstance instancia = crearInstancia(edad, categoria, puntuacion, esFavorito, numCalificaciones);
        if (instancia == null) return "NO";

        double resultado = modelo.classifyInstance(instancia);
        return estructura.classAttribute().value((int) resultado);
    }

    /**
     * Distribución de probabilidad [P(SI), P(NO)].
     * Con RandomForest estos valores son continuos y bien calibrados.
     */
    public double[] probabilidadPrediccion(double edad, String categoria, double puntuacion,
                                           String esFavorito, double numCalificaciones)
            throws Exception {

        if (modelo == null || estructura == null) return new double[]{0.0, 1.0};

        DenseInstance instancia = crearInstancia(edad, categoria, puntuacion, esFavorito, numCalificaciones);
        if (instancia == null) return new double[]{0.0, 1.0};

        return modelo.distributionForInstance(instancia);
    }

    /**
     * Ejecuta k-fold cross-validation sobre el dataset completo
     * y devuelve las métricas reales del modelo.
     *
     * La evaluación usa un RandomForest nuevo entrenado en cada fold,
     * sin tocar el modelo productivo (this.modelo) ya entrenado.
     *
     * @param data  dataset con classIndex ya fijado
     * @param folds número de particiones (típicamente 10)
     */
    public Map<String, Object> evaluarConCrossValidation(Instances data, int folds) throws Exception {

        data.setClassIndex(data.numAttributes() - 1);

        // Clonar configuración del modelo para evaluación
        RandomForest rf = new RandomForest();
        rf.setNumIterations(NUM_ARBOLES);
        rf.setNumFeatures(0);
        rf.setMaxDepth(0);

        Evaluation eval = new Evaluation(data);
        eval.crossValidateModel(rf, data, folds, new Random(1));

        // Índices de clase: 0 = SI, 1 = NO (orden del ARFF: @attribute recomendar {SI,NO})
        double[][] matrix = eval.confusionMatrix();

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("folds",            folds);
        resultado.put("totalInstancias",  (int) data.numInstances());
        resultado.put("correctas",        (int) eval.correct());
        resultado.put("incorrectas",      (int) eval.incorrect());
        resultado.put("accuracy",         r2(eval.pctCorrect()));
        resultado.put("precisionSI",      r2(eval.precision(0) * 100));
        resultado.put("recallSI",         r2(eval.recall(0)    * 100));
        resultado.put("f1SI",             r2(eval.fMeasure(0)  * 100));
        resultado.put("precisionNO",      r2(eval.precision(1) * 100));
        resultado.put("recallNO",         r2(eval.recall(1)    * 100));
        resultado.put("kappa",            r2(eval.kappa()));
        resultado.put("errorMedioAbsoluto", r2(eval.meanAbsoluteError()));

        // Matriz de confusión:  [[verdaderos SI, falsos NO],[falsos SI, verdaderos NO]]
        if (matrix.length >= 2 && matrix[0].length >= 2) {
            resultado.put("VP", (int) matrix[0][0]); // Verdaderos Positivos (SI predicho, SI real)
            resultado.put("FN", (int) matrix[0][1]); // Falsos Negativos    (NO predicho, SI real)
            resultado.put("FP", (int) matrix[1][0]); // Falsos Positivos    (SI predicho, NO real)
            resultado.put("VN", (int) matrix[1][1]); // Verdaderos Negativos(NO predicho, NO real)
        }

        return resultado;
    }

    public boolean listo() {
        return modelo != null;
    }

    public void resetear() {
        this.modelo = null;
        this.estructura = null;
    }

    // ── Privado ──────────────────────────────────────────────────────────────

    private double r2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private DenseInstance crearInstancia(double edad, String categoria, double puntuacion,
                                         String esFavorito, double numCalificaciones) {

        Attribute attrCategoria   = estructura.attribute(1);
        Attribute attrEsFavorito  = estructura.attribute(3);

        int idxCat = attrCategoria.indexOfValue(categoria.toUpperCase());
        if (idxCat == -1) return null;

        int idxFav = attrEsFavorito.indexOfValue(esFavorito.toUpperCase());
        if (idxFav == -1) return null;

        DenseInstance inst = new DenseInstance(6);
        inst.setDataset(estructura);
        inst.setValue(estructura.attribute(0), edad);
        inst.setValue(attrCategoria,           categoria.toUpperCase());
        inst.setValue(estructura.attribute(2), puntuacion);
        inst.setValue(attrEsFavorito,          esFavorito.toUpperCase());
        inst.setValue(estructura.attribute(4), numCalificaciones);
        inst.setMissing(estructura.classIndex());

        return inst;
    }
}
