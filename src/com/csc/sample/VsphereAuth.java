/*
 * Copyright (C) 2016 Computer Science Corporation
 * All rights reserved.
 *
 */
package com.csc.sample;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.xml.ws.BindingProvider;

import com.servicemesh.core.async.Promise;
import com.servicemesh.core.reactor.WorkReactor;
import com.vmware.vim25.InvalidLocaleFaultMsg;
import com.vmware.vim25.InvalidLoginFaultMsg;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ObjectContent;
import com.vmware.vim25.ObjectSpec;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.RetrieveOptions;
import com.vmware.vim25.RetrieveResult;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.SelectionSpec;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.TraversalSpec;
import com.vmware.vim25.UserSession;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VimService;

/**
 * @author gvijayakuma2
 */
public class VsphereAuth
{

    private ServiceContent serviceContent = null;
    private VimPortType vimPortType = null;

    public ServiceContent getServiceContent()
    {
        return serviceContent;
    }

    public static void main(String args[]) throws Exception
    {

        org.apache.log4j.BasicConfigurator.configure();
        VsphereAuth testAuth = new VsphereAuth();
        testAuth.vSphereLogin();
    }

    public void vSphereLogin()
    {

        HostnameVerifier hv = new HostnameVerifier() {
            @Override
            public boolean verify(String urlHostName, SSLSession session)
            {
                return true;
            }
        };
        javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[1];
        javax.net.ssl.TrustManager tm = new TrustAllTrustManager();
        trustAllCerts[0] = tm;
        javax.net.ssl.SSLContext sc = null;
        try
        {
            sc = javax.net.ssl.SSLContext.getInstance("SSL");
        }
        catch (NoSuchAlgorithmException e1)
        {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        javax.net.ssl.SSLSessionContext sslsc = sc.getServerSessionContext();
        sslsc.setSessionTimeout(0);
        try
        {
            sc.init(null, trustAllCerts, null);
        }
        catch (KeyManagementException e1)
        {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier(hv);
        WorkReactor workReactor = WorkReactor.getDefaultWorkReactor();
        Promise<VimPortType> testPromises = Promise.promise(workReactor, () -> authLogin());

        Promise<List<ObjectContent>> vSphereObject = testPromises.flatMap((VimPortType vimPortType) -> getDatacenters());
        //  vSphereObject.onComplete(test -> getDataCenters(vSphereObject, workReactor));

        vSphereObject.onComplete((List<ObjectContent> list) -> {
            List<VSphereObject> vSphereObjects = new ArrayList<VSphereObject>();
            // TODO Auto-generated method stub
            for (ObjectContent objContent : list)
            {
                VSphereObject vSphereObj = new VSphereObject(objContent);
                vSphereObjects.add(vSphereObj);

            }
            System.out.println("vsphere objects" + vSphereObjects);
            for (VSphereObject datacenter : vSphereObjects)
            {
                String dcName = datacenter.getName();
                System.out.println("datacenter name is " + dcName);
            }

            workReactor.shutdown();
        });
        System.out.println("still running");

    }

    private VimPortType authLogin()

    // TODO Auto-generated method stub
    {
        //...long running...
        VimService vimService = new VimService();
        vimPortType = vimService.getVimPort();
        Map<String, Object> ctxt = ((BindingProvider) vimPortType).getRequestContext();
        ctxt.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, "https://192.168.143.21/sdk/vimService");
        ctxt.put(BindingProvider.SESSION_MAINTAIN_PROPERTY, true);

        // Create a ManagedObjectReference to the singleton "ServiceInstance". This is a special reference that
        // can always be built this way
        ManagedObjectReference serviceInstance = new ManagedObjectReference();
        serviceInstance.setType("ServiceInstance");
        serviceInstance.setValue("ServiceInstance");

        // Invoke the RetrieveServiceContent method from the ServiceInstance object.  The method is
        // called from the VimPortType vimPort, with the first argument the object we are calling it with.
        // In Java this would be the equivalent of serviceInstance.retrieveServiceContent()
        //  ServiceContent serviceContent = null;
        try
        {
            serviceContent = vimPortType.retrieveServiceContent(serviceInstance);
        }
        catch (RuntimeFaultFaultMsg e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // serviceContent.getSessionManager() returns the Managed Object Reference for the Session Manager
        // We are calling the "login" method on it, which takes three parameters: a user name, a password, and
        // an optional locale
        try
        {
            UserSession session = vimPortType.login(serviceContent.getSessionManager(), "us\\rnunna", "Good1@good2", null);
            session.getDynamicProperty();

        }
        catch (InvalidLocaleFaultMsg | InvalidLoginFaultMsg | RuntimeFaultFaultMsg e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println(serviceContent.getAbout().getFullName());
        System.out.println("Server type is " + serviceContent.getAbout().getApiType());
        System.out.println("API version is " + serviceContent.getAbout().getVersion());
        return vimPortType;
    }

    private Promise<List<ObjectContent>> getDatacenters()
    {
        // The PropertySpec object specifies what properties
        // to retrieve from what type of Managed Object
        PropertySpec pSpec = new PropertySpec();
        pSpec.setType("Datacenter");
        pSpec.getPathSet().add("name");
        pSpec.getPathSet().add("vmFolder");
        pSpec.getPathSet().add("hostFolder");

        // The following TraversalSpec and SelectionSpec
        // objects create the following relationship:
        //
        // a. Folder -> childEntity
        //   b. recurse to a.
        //
        // This specifies that starting with a Folder
        // managed object, traverse through its childEntity
        // property. For each element in the childEntity
        // property, process by going back to the 'parent'
        // TraversalSpec.
        // SelectionSpec to cause Folder recursion
        SelectionSpec recurseFolders = new SelectionSpec();
        // The name of a SelectionSpec must refer to a
        // TraversalSpec with the same name value.
        recurseFolders.setName("folder2childEntity");
        // Traverse from a Folder through the 'childEntity' property
        TraversalSpec folder2childEntity = new TraversalSpec();
        // Select the Folder type managed object
        folder2childEntity.setType("Folder");
        // Traverse through the childEntity property of the Folder
        folder2childEntity.setPath("childEntity");
        // Name this TraversalSpec so the SelectionSpec above
        // can refer to it
        folder2childEntity.setName(recurseFolders.getName());
        // Add the SelectionSpec above to this traversal so that
        // we will recurse through the tree via the childEntity
        // property
        folder2childEntity.getSelectSet().add(recurseFolders);

        // The ObjectSpec object specifies the starting object and
        // any TraversalSpecs used to specify other objects
        // for consideration
        ObjectSpec oSpec = new ObjectSpec();
        oSpec.setObj(getServiceContent().getRootFolder());
        // We set skip to true because we are not interested
        // in retrieving properties from the root Folder
        oSpec.setSkip(Boolean.TRUE);
        // Specify the TraversalSpec. This is what causes
        // other objects besides the starting object to
        // be considered part of the collection process
        oSpec.getSelectSet().add(folder2childEntity);

        // The PropertyFilterSpec object is used to hold the
        // ObjectSpec and PropertySpec objects for the call
        PropertyFilterSpec pfSpec = new PropertyFilterSpec();
        pfSpec.getPropSet().add(pSpec);
        pfSpec.getObjectSet().add(oSpec);
        // retrieveProperties() returns the properties
        // selected from the PropertyFilterSpec

        List<PropertyFilterSpec> specs = new ArrayList<PropertyFilterSpec>();
        specs.add(pfSpec);

        ServiceContent content = getServiceContent();
        RetrieveOptions options = new RetrieveOptions();
        RetrieveResult results = null;
        try
        {
            results = getVimPortTypeService().retrievePropertiesEx(content.getPropertyCollector(), specs, options);
        }
        catch (InvalidPropertyFaultMsg | RuntimeFaultFaultMsg e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        List<ObjectContent> objs = results.getObjects();
        String token = results.getToken();
        while (token != null && !token.isEmpty())
        {
            try
            {
                results = getVimPortTypeService().continueRetrievePropertiesEx(content.getPropertyCollector(), token);
            }
            catch (InvalidPropertyFaultMsg | RuntimeFaultFaultMsg e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            objs.addAll(results.getObjects());
            token = results.getToken();
        }
        // return VSphereObject.getObjects(objs);
        return Promise.pure(objs);
    }

    private VimPortType getVimPortTypeService()
    {
        // TODO Auto-generated method stub
        return vimPortType;
    }

}
