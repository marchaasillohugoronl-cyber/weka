package com.streamflix.servicio;

import org.springframework.stereotype.Service;
import weka.core.Instance;
import weka.core.Instances;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Exploración del dataset StreamFlix
// Replica el patrón de ExploracionWine y ExploracionCancer
@Service
public class ExplorarServicio {

    private final DatasetServicio datasetServicio;

    public ExplorarServicio(DatasetServicio datasetServicio) {
        this.datasetServicio = datasetServicio;
    }

    // Carga el dataset ARFF generado dinámicamente y retorna su información estructural
    public Map<String, Object> explorar() throws Exception {

        // Cargar dataset desde el ARFF generado con datos reales de la BD
        String arff = datasetServicio.generarARFF();
        Instances data = new Instances(new StringReader(arff));

        // Cantidad de muestras
        int cantMuestras = data.numInstances();

        // Cantidad de columnas (atributos)
        int cantColumnas = data.numAttributes();

        // Listado de atributos
        List<String> atributos = new ArrayList<>();
        for (int i = 0; i < cantColumnas; i++) {
            atributos.add(data.attribute(i).name());
        }

        // Listado de muestras (todas las instancias)
        List<String> muestras = new ArrayList<>();
        for (int i = 0; i < cantMuestras; i++) {
            muestras.add(data.instance(i).toString());
        }

        // Listado de muestras con for-each
        List<String> muestrasForEach = new ArrayList<>();
        for (Instance i : data) {
            muestrasForEach.add(i.toString());
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("cantMuestras",    cantMuestras);
        resultado.put("cantColumnas",    cantColumnas);
        resultado.put("atributos",       atributos);
        resultado.put("muestras",        muestras);
        resultado.put("muestrasForEach", muestrasForEach);

        return resultado;
    }
}
