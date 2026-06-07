package com.streamflix.servicio;

import org.springframework.stereotype.Service;
import weka.classifiers.lazy.IBk;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;

// Clasificación KNN sobre el dataset StreamFlix
// Replica el patrón de WineKNN y CancerKNN
@Service
public class KnnServicio {

    private final DatasetServicio datasetServicio;

    public KnnServicio(DatasetServicio datasetServicio) {
        this.datasetServicio = datasetServicio;
    }

    // Entrena el modelo KNN y realiza una predicción individual
    // Parámetros: edad, categoria, puntuacion, esFavorito, numCalificaciones
    public Map<String, Object> predecirConKnn(double edad, String categoria,
                                               double puntuacion, String esFavorito,
                                               double numCalificaciones) throws Exception {

        // Cargar dataset
        String arff = datasetServicio.generarARFF();
        Instances streamflix = new Instances(new StringReader(arff));

        // Definir variable objetivo (última columna: recomendar)
        streamflix.setClassIndex(streamflix.numAttributes() - 1);

        // Crear modelo KNN
        IBk knn = new IBk();

        // Definir K = 3
        knn.setKNN(3);

        // Entrenar modelo
        knn.buildClassifier(streamflix);

        // Predicción individual
        Instance nueva = new DenseInstance(streamflix.numAttributes());

        // Asociar instancia al dataset
        nueva.setDataset(streamflix);

        nueva.setValue(0, edad);
        nueva.setValue(streamflix.attribute(1),     categoria.toUpperCase());
        nueva.setValue(2, puntuacion);
        nueva.setValue(streamflix.attribute(3),     esFavorito.toUpperCase());
        nueva.setValue(4, numCalificaciones);

        // Obtener índice de la clase con mayor probabilidad
        double indice = knn.classifyInstance(nueva);

        // Convertir índice a nombre de clase
        String clasePredicha = streamflix.classAttribute().value((int) indice);

        // Distribución de probabilidad de cada clase
        double[] distribucion = knn.distributionForInstance(nueva);
        int confianzaPct = (int) Math.round(distribucion[0] * 100);

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("indice",       (int) indice);
        resultado.put("clase",        clasePredicha);
        resultado.put("confianza",    confianzaPct + "%");
        resultado.put("k",            knn.getKNN());
        resultado.put("totalMuestras", streamflix.numInstances());
        resultado.put("edad",         edad);
        resultado.put("categoria",    categoria.toUpperCase());
        resultado.put("puntuacion",   puntuacion);
        resultado.put("esFavorito",   esFavorito.toUpperCase());
        resultado.put("numCal",       (int) numCalificaciones);

        return resultado;
    }
}
