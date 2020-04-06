package es.jbp.comun.reflexion.beans;

/**
 *
 * @author jorge
 */
public interface IParserObjetos {
        
    String toString(Object valor);

    Object toObject(String texto, Class clazz);       
}
