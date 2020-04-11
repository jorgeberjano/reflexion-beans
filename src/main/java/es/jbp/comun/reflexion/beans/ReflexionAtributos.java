package es.jbp.comun.reflexion.beans;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * @author jorge
 */
public class ReflexionAtributos {

    private IParserObjetos parser;

    public ReflexionAtributos(IParserObjetos parser) {
        this.parser = parser;
    }

    /**
     * Obtiene los atributos editables de un objeto javabean en una lista.
     */
    public List<AtributoBean> obtenerListaAtributosEditables(Object objeto) {
        if (objeto == null) {
            return null;
        }

        List<AtributoBean> lista = new ArrayList<>();
        BeanInfo info;
        try {
            info = Introspector.getBeanInfo(objeto.getClass());
        } catch (IntrospectionException ex) {
            return null;
        }
        for (PropertyDescriptor property : info.getPropertyDescriptors()) {
            Method readMethod = property.getReadMethod();
            if (readMethod != null && !"class".equals(property.getName())) {
                Orden orden = readMethod.getAnnotation(Orden.class);
                Editable editable = readMethod.getAnnotation(Editable.class);
                boolean esEditable = editable == null ? true : editable.value();
                if (!esEditable) {
                    continue;
                }

                Object valor;
                try {
                    valor = readMethod.invoke(objeto);
                } catch (Exception ex) {
                    valor = null;
                }
                AtributoBean atributo = new AtributoBean();
                atributo.setNombre(property.getName());
                atributo.setClase(property.getPropertyType());
                atributo.setValor(valor);
                atributo.setOrden(orden == null ? Integer.MAX_VALUE : orden.value());
                lista.add(atributo);
            }
        }
//
//        return lista.stream()
//                .sorted(Comparator.comparingInt(AtributoBean::getOrden))
//                .collect(Collectors.toList());

        Collections.sort(lista, new Comparator<AtributoBean>() {
            @Override
            public int compare(AtributoBean o1, AtributoBean o2) {
                return o1.getOrden() - o2.getOrden();
            }
            
        }); 
        return lista;
    }

    /**
     * Aplica los valores de una lista de atributos sobre un objeto javabean.
     */
    public void aplicarValoresAtributos(List<AtributoBean> listaAtributos, Object bean) throws Exception {
        Map<String, Object> mapa = new HashMap<>();
        //listaAtributos.stream().forEach(atributo -> mapa.put(atributo.getNombre(), atributo.getValor()));
        for (AtributoBean atributo : listaAtributos) {
            mapa.put(atributo.getNombre(), atributo.getValor());
        }
        mapaAObjeto(mapa, bean);
    }
    
    /**
     * Recupera los valores de  un objeto javabea en una lista de atributos 
     */
    public void recuperarValoresAtributos(List<AtributoBean> listaAtributos, Object objeto) {
        Map<String, Object> mapa = objetoAMapa(objeto);
        //listaAtributos.stream().forEach(atributo -> atributo.setValor(mapa.get(atributo.getNombre())));
        if (listaAtributos == null || mapa == null) {
            return;
        }
        for (AtributoBean atributo : listaAtributos) { 
            atributo.setValor(mapa.get(atributo.getNombre()));
        }        
    }

    /**
     * Aplica los valor de una mapa sobre los atribitos de un objeto javabean.
     */
    public void mapaAObjeto(Map<String, Object> mapa, Object objeto) throws Exception {

        if (objeto == null) {
            return;
        }
        BeanInfo info;
        info = Introspector.getBeanInfo(objeto.getClass());
        for (PropertyDescriptor property : info.getPropertyDescriptors()) {
            if (property.getWriteMethod() != null && !"class".equals(property.getName())) {
                if (!mapa.containsKey(property.getName())) {
                    continue;
                }
                Object valor = mapa.get(property.getName());
                asignarValorAtributo(objeto, property, valor);
            }
        }
    }

    public void asignarValorAtributo(Object objeto, String nombreAtributo, Object valor) {

        if (objeto == null || nombreAtributo == null) {
            return;
        }
        try {
            PropertyDescriptor property = new PropertyDescriptor(nombreAtributo, objeto.getClass());
            asignarValorAtributo(objeto, property, valor);
        } catch (Exception ex) {
            System.err.println("No existe el artributo " + nombreAtributo + " de la clase " + objeto.getClass().getName());
        }
    }

    /**
     * Asigna un valor a un atributo de un objeto bean.
     *
     * @param objeto El objeto en el que se realiza la asignación.
     * @param property El atributo al que se le asigna el valor
     * @param valor El valor que se le asigna al atributo
     */
    public void asignarValorAtributo(Object objeto, PropertyDescriptor property, Object valor) {
        try {
            Method metodo = property.getWriteMethod();
            if (metodo == null) {
                System.err.println("No hay un método setter para el atributo " + property.getName());
                return;
            }

            Class[] tipos = metodo.getParameterTypes();
            if (tipos.length != 1) {
                System.err.println("El método " + metodo.getName() + " tiene mas de un parámetro");
                return;
            }
//            Type[] genericParameterTypes = metodo.getGenericParameterTypes();

            Class clazz = tipos[0];
            boolean esColeccion = Collection.class.isAssignableFrom(clazz);
            Object valorTipado = null;

            if (esColeccion && valor != null) {
                if (!(valor instanceof Collection)) {
                    System.err.println("El valor asignado no es una coleccion");
                    return;
                }
                valorTipado = property.getPropertyType().newInstance();
                //((Collection) valorTipado).addAll((Collection) valor);
                agregarValoresAColeccion((Collection) valorTipado, (Collection) valor);
            }

//            if (valor instanceof Collection) {
//                valorTipado = property.getPropertyType().newInstance();
//                ((Collection) valorTipado).addAll((Collection) valor);
//            } else {
//                System.err.println("El valor asignado no es una coleccion");
//                return;
//            }

            if (valorTipado == null) {
                valorTipado = obtenerValorTipado(valor, clazz);
            }

            metodo.invoke(objeto, valorTipado);
        } catch (Exception ex) {
            System.err.println("No se ha le ha podido asignar a la propiedad "
                    + property.getName() + " de la clase " + objeto.getClass().getName()
                    + " el valor " + valor);
            ex.printStackTrace();
        }
    }

    public Object obtenerValorTipado(Object valor, Class clazz) {
        Object valorTipado;
        if (valor == null) {
            valorTipado = null;
        } else if (clazz.isAssignableFrom(valor.getClass())) {
            valorTipado = valor;
        } else if (parser != null) {
            valorTipado = parser.toObject(valor.toString(), clazz);
        } else {
            return null; // No se ha podido convertir
        }
        return valorTipado;
    }

    /**
     * Conviete un objeto en un mapa con una entrada por cada atributo.
     *
     * @param objeto El objeto
     * @return El mapa
     */
    public static Map<String, Object> objetoAMapa(Object objeto) {
        if (objeto == null) {
            return null;
        }

        Map<String, Object> mapa = new LinkedHashMap<>();

        BeanInfo info;
        try {
            info = Introspector.getBeanInfo(objeto.getClass());
        } catch (IntrospectionException ex) {
            return null;
        }
        for (PropertyDescriptor property : info.getPropertyDescriptors()) {
            if (property.getReadMethod() != null && !"class".equals(property.getName())) {
                Object valor;
                try {
                    valor = property.getReadMethod().invoke(objeto);
                } catch (Exception ex) {
                    valor = null;
                }
                mapa.put(property.getName(), valor);
            }
        }
        return mapa;
    }

    private void agregarValoresAColeccion(Collection coleccion, Collection valores) {
        // TODO: covertir los elementos al tipo de la lista
        //coleccion.addAll(valores);
        Class clazz = coleccion.getClass();
        Method metodoAdd = buscarMetodoAdd(clazz);
        if (metodoAdd == null) {
            System.err.println("La colección no tiene un metodo add con un parámetro");
            return;
        }
        Class claseParametro = metodoAdd.getParameterTypes()[0];
        for (Object valor : valores) {
            Object valorTipado = obtenerValorTipado(valor, claseParametro);
            coleccion.add(valorTipado);
        }
    }

    private Method buscarMetodoAdd(Class clazz) {
        Method[] metodos = clazz.getMethods();
        for (Method metodo : metodos) {
            if (metodo.getName().equals("add") && metodo.getParameterCount() == 1) {
                Class claseParametro = metodo.getParameterTypes()[0];
                if (claseParametro == Object.class) {
                    continue;
                }
//                System.out.println("add(" + claseParametro.getName() + ")");

                return metodo;
            }
        }
        return null;
    }

}
