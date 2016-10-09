package com.csc.sample;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.stream.StreamSource;

import com.servicemesh.io.http.HttpClientFactory;
import com.servicemesh.io.http.HttpMethod;
import com.servicemesh.io.http.IHttpClient;
import com.servicemesh.io.http.IHttpClientConfigBuilder;
import com.servicemesh.io.http.IHttpHeader;
import com.servicemesh.io.http.IHttpRequest;
import com.servicemesh.io.http.IHttpResponse;
import com.vmware.vim25.ContinueRetrievePropertiesExRequestType;
import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.LoginRequestType;
import com.vmware.vim25.LoginResponse;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ObjectContent;
import com.vmware.vim25.ObjectSpec;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.RetrieveOptions;
import com.vmware.vim25.RetrievePropertiesExRequestType;
import com.vmware.vim25.RetrievePropertiesExResponse;
import com.vmware.vim25.RetrieveResult;
import com.vmware.vim25.RetrieveServiceContentRequestType;
import com.vmware.vim25.RetrieveServiceContentResponse;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.SelectionSpec;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.TraversalSpec;
import com.vmware.vim25.UserSession;

public class TestAuthIHttp
{

    public static JAXBContext _jaxbContext;

    public static void main(String args[]) throws Exception
    {

        org.apache.log4j.BasicConfigurator.configure();

        _jaxbContext = JAXBContext.newInstance("com.vmware.vim25");

        vSphereTest();
    }

    private static void vSphereTest() throws Exception
    {
        IHttpClient httpClient = null;
        try
        {

            /*
             *   Retrieve Service Content:
             */
            RetrieveServiceContentRequestType reqt = new RetrieveServiceContentRequestType();
            ManagedObjectReference mor = new ManagedObjectReference();
            mor.setType("ServiceInstance");
            mor.setValue("ServiceInstance");
            reqt.setThis(mor);
            QName root = new QName("RetrieveServiceContent");
            JAXBElement<RetrieveServiceContentRequestType> je =
                    new JAXBElement<RetrieveServiceContentRequestType>(root, RetrieveServiceContentRequestType.class, reqt);

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            XMLOutputFactory xof = XMLOutputFactory.newFactory();
            XMLStreamWriter xsw = xof.createXMLStreamWriter(os);
            xsw.writeStartDocument();

            xsw.writeStartElement("Envelope");
            xsw.writeDefaultNamespace("http://schemas.xmlsoap.org/soap/envelope/");
            xsw.writeStartElement("Body");

            Marshaller marshaller = _jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
            marshaller.marshal(je, xsw);

            xsw.writeEndDocument();
            xsw.close();

            IHttpClientConfigBuilder builder = HttpClientFactory.getInstance().getConfigBuilder();

            httpClient = HttpClientFactory.getInstance().getClient(builder.build());

            //   String endpoint = "https://192.168.30.221/sdk/vimService";
            String endpoint = "https://192.168.143.21/sdk/vimService";
            IHttpRequest request = HttpClientFactory.getInstance().createRequest(HttpMethod.POST, new URI(endpoint));

            IHttpHeader contentType = HttpClientFactory.getInstance().createHeader("Content-Type", "text/xml; charset=utf-8");
            request.setHeader(contentType);

            IHttpHeader soapAction = HttpClientFactory.getInstance().createHeader("SOAPAction", "urn:vim25/5.1");
            request.setHeader(soapAction);

            IHttpHeader accept = HttpClientFactory.getInstance().createHeader("Accept", "text/xml, multipart/related");
            request.setHeader(accept);
            request.setContent(os.toString());

            ServiceContent context = null;

            Future<IHttpResponse> future = httpClient.execute(request);
            IHttpResponse httpResponse = future.get();
            if (httpResponse.getStatusCode() == 200)
            {
                String data = httpResponse.getContent();
                System.out.println(data);

                XMLInputFactory xif = XMLInputFactory.newFactory();
                ByteArrayInputStream is = new ByteArrayInputStream(data.getBytes());
                StreamSource xml = new StreamSource(is);
                XMLStreamReader xsr = xif.createXMLStreamReader(xml);
                xsr.nextTag();
                while (!xsr.getLocalName().equals("RetrieveServiceContentResponse") && xsr.hasNext())
                {
                    xsr.nextTag();
                }

                Unmarshaller unmarshaller = _jaxbContext.createUnmarshaller();
                JAXBElement<RetrieveServiceContentResponse> jb =
                        unmarshaller.unmarshal(xsr, RetrieveServiceContentResponse.class);
                xsr.close();

                RetrieveServiceContentResponse rscr = jb.getValue();
                context = rscr.getReturnval();
                System.out.println("Server type is " + context.getAbout().getApiType());
                System.out.println("API version is " + context.getAbout().getVersion());
            }
            else
            {
                System.out.println("Http Status: " + httpResponse.getStatusCode());
                return;
            }

            /*
             *
             * Login
             */
            ManagedObjectReference sm = context.getSessionManager();
            LoginRequestType login = new LoginRequestType();
            login.setThis(sm);

            // login.setUserName("administrator");
            //login.setPassword("x0c!0ud");

            login.setUserName("us\\rnunna");
            login.setPassword("Good1@good2");

            root = new QName("Login");
            JAXBElement<LoginRequestType> jeLogin = new JAXBElement<LoginRequestType>(root, LoginRequestType.class, login);

            os = new ByteArrayOutputStream();
            xsw = xof.createXMLStreamWriter(os);
            xsw.writeStartDocument();

            xsw.writeStartElement("Envelope");
            xsw.writeDefaultNamespace("http://schemas.xmlsoap.org/soap/envelope/");
            xsw.writeStartElement("Body");

            marshaller = _jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
            marshaller.marshal(jeLogin, xsw);

            xsw.writeEndDocument();
            xsw.close();

            request = HttpClientFactory.getInstance().createRequest(HttpMethod.POST, new URI(endpoint));
            request.setHeader(contentType);
            request.setHeader(soapAction);
            request.setHeader(accept);

            request.setContent(os.toString());

            UserSession userSession = null;

            future = httpClient.execute(request);
            httpResponse = future.get();
            if (httpResponse.getStatusCode() == 200)
            {
                String data = httpResponse.getContent();
                System.out.println(data);

                XMLInputFactory xif = XMLInputFactory.newFactory();
                ByteArrayInputStream is = new ByteArrayInputStream(data.getBytes());
                StreamSource xml = new StreamSource(is);
                XMLStreamReader xsr = xif.createXMLStreamReader(xml);
                xsr.nextTag();
                while (!xsr.getLocalName().equals("LoginResponse") && xsr.hasNext())
                {
                    xsr.nextTag();
                }

                Unmarshaller unmarshaller = _jaxbContext.createUnmarshaller();
                JAXBElement<LoginResponse> jb = unmarshaller.unmarshal(xsr, LoginResponse.class);
                xsr.close();

                LoginResponse lresp = jb.getValue();
                userSession = lresp.getReturnval();

            }
            else
            {
                System.out.println("Http Status: " + httpResponse.getStatusCode());
                decodeFaultMessage(httpResponse.getContent(), "faultstring");
                return;
            }

            //  * Get Data Centers:

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
            oSpec.setObj(context.getRootFolder());
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

            RetrieveOptions options = new RetrieveOptions();
            RetrievePropertiesExRequestType rex = new RetrievePropertiesExRequestType();
            rex.setThis(context.getPropertyCollector());
            rex.getSpecSet().addAll(specs);
            rex.setOptions(options);

            root = new QName("RetrievePropertiesEx");
            JAXBElement<RetrievePropertiesExRequestType> jeProp =
                    new JAXBElement<RetrievePropertiesExRequestType>(root, RetrievePropertiesExRequestType.class, rex);

            os = new ByteArrayOutputStream();
            xsw = xof.createXMLStreamWriter(os);
            xsw.writeStartDocument();

            xsw.writeStartElement("Envelope");
            xsw.writeDefaultNamespace("http://schemas.xmlsoap.org/soap/envelope/");
            xsw.writeStartElement("Body");

            marshaller = _jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
            marshaller.marshal(jeProp, xsw);

            xsw.writeEndDocument();
            xsw.close();

            request = HttpClientFactory.getInstance().createRequest(HttpMethod.POST, new URI(endpoint));
            request.setHeader(contentType);
            request.setHeader(soapAction);
            request.setHeader(accept);

            request.setContent(os.toString());

            future = httpClient.execute(request);
            httpResponse = future.get();
            if (httpResponse.getStatusCode() == 200)
            {
                String data = httpResponse.getContent();
                System.out.println(data);

                XMLInputFactory xif = XMLInputFactory.newFactory();
                ByteArrayInputStream is = new ByteArrayInputStream(data.getBytes());
                StreamSource xml = new StreamSource(is);
                XMLStreamReader xsr = xif.createXMLStreamReader(xml);
                xsr.nextTag();
                while (!xsr.getLocalName().equals("RetrievePropertiesExResponse") && xsr.hasNext())
                {
                    xsr.nextTag();
                }

                if (xsr.hasNext() == false)
                {
                    return;
                }

                Unmarshaller unmarshaller = _jaxbContext.createUnmarshaller();
                JAXBElement<RetrievePropertiesExResponse> jb = unmarshaller.unmarshal(xsr, RetrievePropertiesExResponse.class);
                xsr.close();

                RetrievePropertiesExResponse resp = jb.getValue();
                RetrieveResult result = resp.getReturnval();
                List<ObjectContent> objs = result.getObjects();
                ManagedObjectReference vmFolder = null;
                for (ObjectContent obj : objs)
                {

                    System.out.println(obj.getObj().getType() + " - " + obj.getObj().getValue());
                    for (DynamicProperty prop : obj.getPropSet())
                    {
                        System.out.println("   " + prop.getName() + " - " + prop.getVal().toString());
                        if (prop.getName().equals("vmFolder"))
                        {
                            vmFolder = (ManagedObjectReference) prop.getVal();
                            break;
                        }

                    }
                }

                //invoke for VM's

                List<PropertyFilterSpec> propSpec = getVirtualMachines(vmFolder);

                RetrieveOptions optionss = new RetrieveOptions();
                RetrievePropertiesExRequestType rexx = new RetrievePropertiesExRequestType();
                rexx.setThis(context.getPropertyCollector());
                rexx.getSpecSet().addAll(propSpec);
                rexx.setOptions(optionss);

                root = new QName("RetrievePropertiesEx");
                JAXBElement<RetrievePropertiesExRequestType> jePropp =
                        new JAXBElement<RetrievePropertiesExRequestType>(root, RetrievePropertiesExRequestType.class, rexx);

                os = new ByteArrayOutputStream();
                xsw = xof.createXMLStreamWriter(os);
                xsw.writeStartDocument();

                xsw.writeStartElement("Envelope");
                xsw.writeDefaultNamespace("http://schemas.xmlsoap.org/soap/envelope/");
                xsw.writeStartElement("Body");

                marshaller = _jaxbContext.createMarshaller();
                marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
                marshaller.marshal(jePropp, xsw);

                xsw.writeEndDocument();
                xsw.close();

                request = HttpClientFactory.getInstance().createRequest(HttpMethod.POST, new URI(endpoint));
                request.setHeader(contentType);
                request.setHeader(soapAction);
                request.setHeader(accept);

                request.setContent(os.toString());

                future = httpClient.execute(request);
                httpResponse = future.get();
                if (httpResponse.getStatusCode() == 200)
                {
                    String datas = httpResponse.getContent();
                    // System.out.println(datas);
                    RetrievePropertiesExResponse respp = decodeResponse(datas);
                    RetrieveResult resultt = respp.getReturnval();
                    System.out.println("token " + resultt.getToken());
                    String token = resultt.getToken();
                    List<ObjectContent> objss = resultt.getObjects();
                    System.out.println("objss list *** " + objss.size());
                    while (token != null && !token.isEmpty())
                    {
                        ContinueRetrievePropertiesExRequestType continueRequestType =
                                new ContinueRetrievePropertiesExRequestType();
                        continueRequestType.setThis(context.getPropertyCollector());
                        continueRequestType.setToken(token);

                        QName roots = new QName("ContinueRetrievePropertiesEx");
                        JAXBElement<ContinueRetrievePropertiesExRequestType> jeProppp =
                                new JAXBElement<ContinueRetrievePropertiesExRequestType>(roots,
                                        ContinueRetrievePropertiesExRequestType.class, continueRequestType);

                        os = new ByteArrayOutputStream();
                        xsw = xof.createXMLStreamWriter(os);
                        xsw.writeStartDocument();

                        xsw.writeStartElement("Envelope");
                        xsw.writeDefaultNamespace("http://schemas.xmlsoap.org/soap/envelope/");
                        xsw.writeStartElement("Body");

                        marshaller = _jaxbContext.createMarshaller();
                        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
                        marshaller.marshal(jeProppp, xsw);

                        xsw.writeEndDocument();
                        xsw.close();

                        request = HttpClientFactory.getInstance().createRequest(HttpMethod.POST, new URI(endpoint));
                        request.setHeader(contentType);
                        request.setHeader(soapAction);
                        request.setHeader(accept);

                        request.setContent(os.toString());

                        future = httpClient.execute(request);
                        httpResponse = future.get();
                        if (httpResponse.getStatusCode() == 200)
                        {
                            String datass = httpResponse.getContent();
                            System.out.println(datass);
                            RetrievePropertiesExResponse resppp = decodeResponse(datas);
                            RetrieveResult resulttt = resppp.getReturnval();
                            System.out.println("token " + resulttt.getToken());
                            token = resulttt.getToken();
                            List<ObjectContent> objsss = resulttt.getObjects();
                            System.out.println("objss list *** " + objsss.size());
                        }
                        else
                        {
                            token = null;
                        }

                    }

                }

            }
            else
            {
                System.out.println("Http Status: " + httpResponse.getStatusCode());
                return;
            }

        }
        catch (Exception ex)
        {
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        finally
        {
            if (httpClient != null)
            {
                httpClient.close();
            }
        }
    }

    public static Object decodeFaultMessage(String xml, String namespace)
            throws XMLStreamException, FactoryConfigurationError, JAXBException
    {

        XMLStreamReader xsr =
                XMLInputFactory.newFactory().createXMLStreamReader(new StreamSource(new ByteArrayInputStream(xml.getBytes())));
        xsr.nextTag();

        while (!xsr.getLocalName().equals(namespace) && xsr.hasNext())
        {
            System.out.println("xsr.getLocalName" + xsr.getLocalName());
            xsr.nextTag();
        }
        System.out.println("xsr.getLocalNames  ==" + xsr.getLocalName());
        System.out.println("test" + xsr.getElementText());
        //  System.out.println("test" + xsr.getTextCharacters());
        System.out.println("xsr string" + xsr.getText());

        //once it matches get the fault message

        xsr.close();

        return null;

    }

    public static List<PropertyFilterSpec> getVirtualMachines(ManagedObjectReference folderRef)
            throws InvalidPropertyFaultMsg, RemoteException, RuntimeFaultFaultMsg, Exception
    {
        // The PropertySpec object specifies what properties
        // retrieve from what type of Managed Object
        PropertySpec pSpec = new PropertySpec();
        pSpec.setType("VirtualMachine");
        pSpec.getPathSet().add("name");
        pSpec.getPathSet().add("parent");
        pSpec.getPathSet().add("config.changeVersion");
        pSpec.getPathSet().add("config.uuid");
        pSpec.getPathSet().add("config.hardware.memoryMB");
        pSpec.getPathSet().add("config.hardware.numCPU");
        pSpec.getPathSet().add("config.hardware.numCoresPerSocket");
        pSpec.getPathSet().add("config.hardware.device");
        pSpec.getPathSet().add("config.guestFullName");
        pSpec.getPathSet().add("config.guestId");
        pSpec.getPathSet().add("config.hotPlugMemoryLimit");
        pSpec.getPathSet().add("config.instanceUuid");
        pSpec.getPathSet().add("config.template");
        pSpec.getPathSet().add("config.memoryHotAddEnabled");
        pSpec.getPathSet().add("config.cpuHotAddEnabled");
        pSpec.getPathSet().add("config.cpuHotRemoveEnabled");
        pSpec.getPathSet().add("config.managedBy.extensionKey");
        pSpec.getPathSet().add("config.extraConfig");
        pSpec.getPathSet().add("runtime.powerState");
        pSpec.getPathSet().add("runtime.host");
        pSpec.getPathSet().add("snapshot");
        pSpec.getPathSet().add("guest.ipAddress");
        pSpec.getPathSet().add("guest.toolsRunningStatus");
        pSpec.getPathSet().add("guest.hostName");
        pSpec.getPathSet().add("guest.net"); //  Array of assigned addresses

        // Traverse from a Datacenter through the 'vmFolder' property
        TraversalSpec folder2childEntity = new TraversalSpec();
        folder2childEntity.setType("Folder");
        folder2childEntity.setPath("childEntity");
        folder2childEntity.setName("folder2childEntity");

        ObjectSpec oSpec = new ObjectSpec();
        oSpec.setObj(folderRef);
        oSpec.setSkip(Boolean.TRUE);
        oSpec.getSelectSet().add(folder2childEntity);

        PropertyFilterSpec pfSpec = new PropertyFilterSpec();
        pfSpec.getPropSet().add(pSpec);
        pfSpec.getObjectSet().add(oSpec);

        List<PropertyFilterSpec> specs = new ArrayList<PropertyFilterSpec>();
        specs.add(pfSpec);

        return specs;
    }

    public static RetrievePropertiesExResponse decodeResponse(String datas) throws XMLStreamException, JAXBException
    {
        XMLInputFactory xiff = XMLInputFactory.newFactory();
        ByteArrayInputStream iss = new ByteArrayInputStream(datas.getBytes());
        StreamSource xmll = new StreamSource(iss);
        XMLStreamReader xsrr = xiff.createXMLStreamReader(xmll);
        xsrr.nextTag();
        while (!xsrr.getLocalName().equals("RetrievePropertiesExResponse") && xsrr.hasNext())
        {
            xsrr.nextTag();
        }

        if (xsrr.hasNext() == false)
        {
            return null;
        }

        Unmarshaller unmarshallerr = _jaxbContext.createUnmarshaller();
        JAXBElement<RetrievePropertiesExResponse> jbb = unmarshallerr.unmarshal(xsrr, RetrievePropertiesExResponse.class);
        xsrr.close();

        return jbb.getValue();

    }

}