package com.acgist.taoyao.signal.client.gb;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.acgist.taoyao.signal.client.gb.GbDevice.Message;
import com.acgist.taoyao.signal.client.gb.GbDevice.Notify;
import com.acgist.taoyao.signal.client.gb.GbDevice.Query;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GbXML {

    private static final XmlMapper xmlMapper = new XmlMapper();

    static {
        xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        xmlMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public enum MessageType {

        QUERY,
        NOTIFY,
        RESPONSE,

    }

    public static final String notify(String cmdType, String deviceId, String status) {
        final Notify notify = new Notify();
        notify.setCmdType(cmdType);
        notify.setSn(String.valueOf(System.currentTimeMillis()));
        notify.setDeviceId(deviceId);
        notify.setStatus(status);
        return GbXML.write(notify);
    }

    public static final String query(String cmdType, String deviceId) {
        final Query query = new Query();
        query.setCmdType(cmdType);
        query.setSn(String.valueOf(System.currentTimeMillis()));
        query.setDeviceId(deviceId);
        return GbXML.write(query);
    }

    public static final String write(Object object) {
        try {
            return xmlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("XML转换失败", e);
        }
        return null;
    }

    public static final Query query(String xml) {
        try {
            return xmlMapper.readValue(xml, Query.class);
        } catch (JsonProcessingException e) {
            log.error("XML转换失败", e);
        }
        return null;
    }

    public static final Message message(String xml) {
        try {
            return xmlMapper.readValue(xml, Message.class);
        } catch (JsonProcessingException e) {
            log.error("XML转换失败", e);
        }
        return null;
    }

    public static final MessageType messageType(String xml) {
        try {
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder builder = factory.newDocumentBuilder();
            final Document document = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            final Element root = document.getDocumentElement();
            return switch (root.getTagName()) {
                case "Query"    -> MessageType.QUERY;
                case "Notify"   -> MessageType.NOTIFY;
                case "Response" -> MessageType.RESPONSE;
                default         -> null;
            };
        } catch (IOException | SAXException | ParserConfigurationException e) {
            log.error("XML解析失败", e);
        }
        return null;
    }

}
