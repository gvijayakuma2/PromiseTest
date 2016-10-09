package com.csc.sample;

import java.util.HashMap;
import java.util.List;

import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ObjectContent;

public class VSphereObject
{
    private ManagedObjectReference _object;
    private HashMap<String, Object> _properties = new HashMap<String, Object>();

    public VSphereObject(ObjectContent oc)
    {
        _object = oc.getObj();
        List<DynamicProperty> props = oc.getPropSet();
        if (props != null)
        {
            for (DynamicProperty p : props)
            {
                _properties.put(p.getName(), p.getVal());
            }
        }
    }

    public ManagedObjectReference getObject()
    {
        return _object;
    }

    public Object getProperty(String name)
    {
        return _properties.get(name);
    }

    public String getName()
    {
        return (String) _properties.get("name");
    }

    public String getString(String name)
    {
        return (String) _properties.get(name);
    }

    public static VSphereObject[] getObjects(List<ObjectContent> oc)
    {
        if (oc != null)
        {
            VSphereObject[] objects = new VSphereObject[oc.size()];
            int i = 0;
            for (ObjectContent o : oc)
            {
                objects[i++] = new VSphereObject(o);
            }
            return objects;
        }
        else
        {
            return new VSphereObject[0];
        }
    }
}
