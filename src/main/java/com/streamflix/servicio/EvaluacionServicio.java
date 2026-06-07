package com.streamflix.servicio;

import org.springframework.stereotype.Service;
import weka.classifiers.Evaluation;
import weka.classifiers.lazy.IBk;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

// Evaluación del modelo con split 80/20 y predicción individual
// Replica el patrón de WinePauta y CancerPauta
@Service
public class EvaluacionServicio {

    private final DatasetServicio datasetServicio;

    public EvaluacionServicio(DatasetServicio datasetServicio) {
        this.datasetServicio = datasetServicio;
    }

    // Entrena KNN con split 80/20, evalúa y predice un caso de ejemplo
    public Map<String, Object> evaluarConSplit() throws Exception {

        // Carga del dataset StreamFlix
        String arff = datasetServicio.generarARFF();
        Instances streamflix = new Instances(new StringReader(arff));
        streamflix.setClassIndex(streamflix.numAttributes() - 1);

        // Mezclar datos aleatoriamente para evitar sesgo de orden
        streamflix.randomize(new Random(1));

        // Split 80% entrenamiento - 20% prueba
        int trainSize = (int) Math.round(streamflix.numInstances() * 0.8);
        int testSize  = streamflix.numInstances() - trainSize;

        // Crear conjuntos de entrenamiento y prueba
        Instances train = new Instances(streamflix, 0, trainSize);
        Instances test  = new Instances(streamflix, trainSize, testSize);

        // Modelo KNN (IBk) con K = 3
        IBk knn = new IBk();
        knn.setKNN(3);

        // Entrenar el modelo con el conjunto de entrenamiento
        knn.buildClassifier(train);

        // Evaluación sobre el conjunto de prueba
        Evaluation eval = new Evaluation(train);
        eval.evaluateModel(knn, test);

        // Accuracy: porcentaje de instancias clasificadas correctamente
        double accuracy = (1 - eval.errorRate()) * 100;

        // Predicción individual de ejemplo (usuario de 28 años, ACCION, puntaje 4, favorito, 3 calificadas)
        Instance nueva = new DenseInstance(train.numAttributes());
        nueva.setDataset(train);
        nueva.setValue(0, 28);
        nueva.setValue(train.attribute(1), "ACCION");
        nueva.setValue(2, 4.0);
        nueva.setValue(train.attribute(3), "SI");
        nueva.setValue(4, 3);

        // Obtener índice de la clase predicha
        double indice = knn.classifyInstance(nueva);

        // Convertir índice a nombre de clase
        String clasePredicha = train.classAttribute().value((int) indice);

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("totalMuestras", streamflix.numInstances());
        resultado.put("trainSize",     trainSize);
        resultado.put("testSize",      testSize);
        resultado.put("k",             knn.getKNN());
        resultado.put("accuracy",      Math.round(accuracy * 100.0) / 100.0);
        resultado.put("precision",     Math.round(eval.precision(0) * 10000.0) / 100.0);
        resultado.put("recall",        Math.round(eval.recall(0)    * 10000.0) / 100.0);
        resultado.put("resumen",       eval.toSummaryString("=== Resumen KNN ===", false));
        resultado.put("claseEjemplo",  clasePredicha);
        resultado.put("indiceEjemplo", (int) indice);

        return resultado;
    }
}
