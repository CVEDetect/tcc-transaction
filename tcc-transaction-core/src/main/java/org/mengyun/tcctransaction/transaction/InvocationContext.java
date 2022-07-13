package org.mengyun.tcctransaction.transaction;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by changmingxie on 11/9/15.
 */
public class InvocationContext implements Serializable {

    private static final long serialVersionUID = -7969140711432461165L;
    private final Map<String, String> attachments = new ConcurrentHashMap<String, String>();
    private Class targetClass;
    private String methodName;
    private Class[] parameterTypes;
    private Object[] args;

    public InvocationContext() {

    }

    public InvocationContext(Class targetClass, String methodName, Class[] parameterTypes, Object... args) {
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
        this.targetClass = targetClass;
        this.args = args;
    }

    public Object[] getArgs() {
        return args;
    }

    public Class getTargetClass() {
        return targetClass;
    }

    public String getMethodName() {
        return methodName;
    }

    public Class[] getParameterTypes() {
        return parameterTypes;
    }

    public void addAttachment(String key, String value) {
        attachments.put(key, value);
    }
}