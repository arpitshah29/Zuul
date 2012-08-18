package org.devnull.zuul.web

import org.devnull.zuul.data.model.Environment
import org.devnull.zuul.data.model.SettingsEntry
import org.devnull.zuul.data.model.SettingsGroup
import org.devnull.zuul.service.ZuulService
import org.junit.Before
import org.junit.Test
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.web.multipart.MultipartFile

import static org.mockito.Mockito.*

class SettingsServicesControllerTest {

    SettingsServicesController controller

    @Before
    void createController() {
        controller = new SettingsServicesController(zuulService: mock(ZuulService))
    }

    @Test
    void createFromPropertiesFileShouldInvokeServiceWithFileInputStream() {
        def multipartFile = mock(MultipartFile)
        def inputStream = mock(InputStream)
        when(multipartFile.getInputStream()).thenReturn(inputStream)
        def view = controller.createFromProperties(multipartFile, "foo", "dev")
        verify(controller.zuulService).createSettingsGroupFromPropertiesFile("foo", "dev", inputStream)
        assert view == "redirect:/settings/foo#dev"
    }

    @Test
    void renderPropertiesByNameAndEnvRenderPropertiesFile() {
        def mockResponse = new MockHttpServletResponse()
        def group = new SettingsGroup(name: "my-application", environment: new Environment(name: "dev"))
        group.entries.add(new SettingsEntry(key: "jdbc.driver", value: "com.awesome.db.Driver"))
        group.entries.add(new SettingsEntry(key: "jdbc.username", value: "maxPower"))

        when(controller.zuulService.findSettingsGroupByNameAndEnvironment(group.name, group.environment.name)).thenReturn(group)
        controller.renderPropertiesByNameAndEnv(mockResponse, group.name, group.environment.name)
        verify(controller.zuulService).findSettingsGroupByNameAndEnvironment(group.name, group.environment.name)

        def content = mockResponse.contentAsString
        assert content
        println content
        def properties = new Properties()
        properties.load(new StringReader(content))
        assert properties['jdbc.driver'] == "com.awesome.db.Driver"
        assert properties['jdbc.username'] == "maxPower"
    }

    @Test
    void deletePropertiesByNameAndEnvShouldInvokeServiceAndReturnCorrectResponseCode() {
        def response = new MockHttpServletResponse()
        def group = new SettingsGroup(id:123)
        when(controller.zuulService.findSettingsGroupByNameAndEnvironment("test-config", "dev")).thenReturn(group)
        controller.deletePropertiesByNameAndEnv(response, "test-config", "dev")
        verify(controller.zuulService).deleteSettingsGroup(123)
        assert response.status == 204
    }


    @Test
    void listJsonShouldReturnResultsFromService() {
        def expected = [new SettingsGroup(name: "a")]
        when(controller.zuulService.listSettingsGroups()).thenReturn(expected)
        def results = controller.listJson(true)
        verify(controller.zuulService).listSettingsGroups()
        assert results.is(expected)
    }

    @Test
    void encryptionShouldReturnResultsFromService() {
        def expected = new SettingsEntry(id: 1, key: "a.b.c", value: "foo", encrypted: false)
        when(controller.zuulService.encryptSettingsEntryValue(expected.id)).thenReturn(expected)
        def result = controller.encrypt(expected.id)
        verify(controller.zuulService).encryptSettingsEntryValue(expected.id)
        assert result.is(expected)
    }

    @Test
    void decryptionShouldReturnResultsFromService() {
        def expected = new SettingsEntry(id: 1, key: "a.b.c", value: "foo", encrypted: true)
        when(controller.zuulService.decryptSettingsEntryValue(expected.id)).thenReturn(expected)
        def result = controller.decrypt(expected.id)
        verify(controller.zuulService).decryptSettingsEntryValue(expected.id)
        assert result.is(expected)
    }

    @Test
    void showEntryJsonShouldReturnResultsFromService() {
        def expected = new SettingsEntry(id: 1)
        when(controller.zuulService.findSettingsEntry(1)).thenReturn(expected)
        def result = controller.showEntryJson(1)
        assert result.is(expected)
    }

    @Test
    void updateEntryJsonShouldBindToResultsFromService() {
        def formEntry = new SettingsEntry(id: -1, key: "a", value: "b")
        def persistedEntry = new SettingsEntry(id: 100, key: "c", value: "d")
        when(controller.zuulService.findSettingsEntry(100)).thenReturn(persistedEntry)
        when(controller.zuulService.save(persistedEntry)).thenReturn(persistedEntry)
        def resultEntry = controller.updateEntryJson(100, formEntry)
        assert resultEntry.is(persistedEntry)
        assert resultEntry.id == 100
        assert resultEntry.key == "a"
        assert resultEntry.value == "b"

    }

    @Test
    void deleteEntryJsonShouldInvokeServiceAndReturnCorrectResponseCode() {
        def response = new MockHttpServletResponse()
        controller.deleteEntryJson(123, response)
        verify(controller.zuulService).deleteSettingsEntry(123)
        assert response.status == 204
    }

}