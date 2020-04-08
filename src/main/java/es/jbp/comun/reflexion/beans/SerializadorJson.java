package es.jbp.comun.reflexion.beans;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jorge
 */
public class SerializadorJson {

    private Gson gson = new Gson();
    private Map<String, Class> mapaAliasClase = new HashMap<>();
    private Map<Class, String> mapaClaseAlias = new HashMap<>();

    private IParserObjetos parser;
    private ReflexionAtributos reflexion;
    
    public SerializadorJson(IParserObjetos parser) {
        this.parser = parser;
        reflexion = new ReflexionAtributos(parser);
    }

    public void asignarNombreClase(String alias, Class clase) {
        mapaAliasClase.put(alias, clase);
        mapaClaseAlias.put(clase, alias);
    }

    public String getNombreDeClase(Class clazz) {
        String nombre = mapaClaseAlias.get(clazz);
        if (nombre == null) {
            return clazz.getCanonicalName();
        }
        return nombre;
    }
    
    public Class getClaseDeNombre(String nombre) {
        Class clazz = mapaAliasClase.get(nombre);      
        if (clazz == null) {
            try {
                clazz = Class.forName(nombre);
            } catch (ClassNotFoundException ex) {
            }
        }
        return clazz;
    }

    public String toJson(Object objeto) {
        JsonElement elemento = objectToJsonElement(objeto);
        return serializar(elemento);
    }

    private JsonElement objectToJsonElement(Object objeto) {
        if (objeto == null) {
            return JsonNull.INSTANCE;
        }

        if (objeto instanceof Collection) {
            return collectionToJsonArray((Collection) objeto);
        }
        Map<String, Object> mapa = ReflexionAtributos.objetoAMapa(objeto);
        mapa.put("clase", getNombreDeClase(objeto.getClass()));
        return mapToJsonObject(mapa);
    }

    private JsonArray collectionToJsonArray(Collection lista) {
        JsonArray jsonArray = new JsonArray();
        for (Object elemento : lista) {
            if (elemento instanceof Boolean) {
                jsonArray.add((Boolean) elemento);
            } else if (elemento instanceof Number) {
                jsonArray.add((Number) elemento);
            } else if (elemento instanceof Character) {
                jsonArray.add((Character) elemento);
            } else if (elemento instanceof String) {
                jsonArray.add((String) elemento);
            } else {
                String valorString = parser.toString(elemento);
                if (valorString != null) {
                    jsonArray.add(valorString);
                } else {
                    JsonElement jsonObject = objectToJsonElement(elemento);
                    jsonArray.add(jsonObject);
                }
            }
        }
        return jsonArray;
    }

    private JsonObject mapToJsonObject(Map mapa) {
        JsonObject jsonObject = new JsonObject();
        for (Object key : mapa.keySet()) {
            String atributo = key.toString();
            Object valor = mapa.get(atributo);

            if (valor instanceof Boolean) {
                jsonObject.addProperty(atributo, (Boolean) valor);
            } else if (valor instanceof Number) {
                jsonObject.addProperty(atributo, (Number) valor);
            } else if (valor instanceof Character) {
                jsonObject.addProperty(atributo, (Character) valor);
            } else if (valor instanceof String) {
                jsonObject.addProperty(atributo, (String) valor);
            } else {
                String valorParseado = parser.toString(valor);
                if (valorParseado == null) {
                    jsonObject.add(atributo, objectToJsonElement(valor));
                } else {
                    jsonObject.addProperty(atributo, valorParseado);
                }
            }
        }
        return jsonObject;
    }

    private String serializar(JsonElement elemento) {
        Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
        return gson.toJson(elemento);
    }

    public Object jsonToObject(String json) {

        if (json == null) {
            return null;
        }
        JsonObject jsonObject = gson.fromJson(json, JsonObject.class);        
        return jsonObjectToObject(jsonObject);
    }    
    
    private Object jsonObjectToObject(JsonObject jsonObject) {        
        String clase = jsonObject.get("clase").getAsString();        
        Class clazz = getClaseDeNombre(clase);
        Object object;
        try {
            object = clazz.newInstance();
        } catch (Exception ex) {
            return null;
        }        
        for (String atributo : jsonObject.keySet()) {
            JsonElement elemento = jsonObject.get(atributo);
            // Aqui hay que transformar el valor en funcion del tipo de atributo
            Object valor = jsonElementToObject(elemento);
            //Class clazz = reflexion.obtenerClaseAtributo(object, atributo);
            reflexion.asignarValorAtributo(object, atributo, valor);
        }
        return object;
    }
    
    private List jsonArrayToList(JsonArray jsonArray) {
        List lista = new ArrayList();
        for (JsonElement elemento : jsonArray) {
            Object valor = jsonElementToObject(elemento);
            lista.add(valor);
        }
        return lista;
    }
    
    private Object jsonElementToObject(JsonElement elemento) {
        Object valor = null;
        if (elemento.isJsonArray()) {
            valor = jsonArrayToList(elemento.getAsJsonArray());
        } else if (elemento.isJsonNull()) {
            valor = null;
        } else if (elemento.isJsonObject()) {
            valor = jsonObjectToObject(elemento.getAsJsonObject());                
        } else if (elemento.isJsonPrimitive()) {
            JsonPrimitive primitive = elemento.getAsJsonPrimitive();                
            if (primitive.isBoolean()) {
                valor = primitive.getAsBoolean();
            } else if (primitive.isNumber()) {
                valor = primitive.getAsNumber();
            } else if (primitive.isString()) {
                valor = primitive.getAsString();
            }
        }
        return valor;
    }
}
