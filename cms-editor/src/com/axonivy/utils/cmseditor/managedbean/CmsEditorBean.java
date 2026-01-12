package com.axonivy.utils.cmseditor.managedbean;

import static ch.ivyteam.ivy.environment.Ivy.cms;
import static java.lang.Integer.valueOf;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static javax.faces.application.FacesMessage.SEVERITY_INFO;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.primefaces.PF;
import org.primefaces.PrimeFaces;
import org.primefaces.model.StreamedContent;

import com.axonivy.utils.cmseditor.dto.CmsValueDto;
import com.axonivy.utils.cmseditor.model.Cms;
import com.axonivy.utils.cmseditor.model.CmsContent;
import com.axonivy.utils.cmseditor.model.PmvCms;
import com.axonivy.utils.cmseditor.model.SavedCms;
import com.axonivy.utils.cmseditor.repo.SavedCmsRepo;
import com.axonivy.utils.cmseditor.service.CmsService;
import com.axonivy.utils.cmseditor.utils.CmsFileUtils;
import com.axonivy.utils.cmseditor.utils.FacesContexts;
import com.axonivy.utils.cmseditor.utils.Utils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.ivyteam.ivy.application.ActivityState;
import ch.ivyteam.ivy.application.IActivity;
import ch.ivyteam.ivy.application.IApplication;
import ch.ivyteam.ivy.application.IProcessModel;
import ch.ivyteam.ivy.application.app.IApplicationRepository;
import ch.ivyteam.ivy.cm.ContentObject;
import ch.ivyteam.ivy.cm.ContentObjectReader;
import ch.ivyteam.ivy.cm.ContentObjectValue;
import ch.ivyteam.ivy.cm.exec.ContentManagement;
import ch.ivyteam.ivy.security.ISecurityContext;

@ViewScoped
@ManagedBean
public class CmsEditorBean implements Serializable {
  private static final long serialVersionUID = 1L;

  private static final String CONTENT_FORM_SELECTED_URL = "content-form:selected-url";
  private static final String CONTENT_FORM_CMS_VALUES = "content-form:cms-values";
  private static final String CONTENT_FORM_TABLE_CMS_KEYS = "content-form:table-cms-keys";
  private static final String CMS_EDITOR_PMV_NAME = "cms-editor";
  private static final String CMS_EDITOR_DEMO_PMV_NAME = "cms-editor-demo";
  private static final String CONTENT_FORM_LINK_COLUMN = "content-form:link-column";
  private static final String CONTENT_FORM_EDITABLE_COLUMN = "content-form:editable-column";
  private static final String CONTENT_FORM_CMS_EDIT_VALUE = "content-form:cms-edit-value";
  private static final String CONTENT_FORM = "content-form";
  private static final String OPEN_SUCCESS_DIALOG_SCRIPT = "showDialog('SaveSuccessDlg');";
  private static final ObjectMapper mapper = new ObjectMapper();
  
  private Map<String, Map<String, SavedCms>> savedCmsMap;
  private List<Cms> cmsList;
  private List<Cms> filteredCMSList;
  private Cms lastSelectedCms;
  private Cms selectedCms;
  private String selectedProjectName;
  private String searchKey;
  private StreamedContent fileDownload;
  private boolean isShowEditorCms;
  private Map<String, PmvCms> pmvCmsMap;
  private boolean isEditableCms;

  @PostConstruct
  private void init() {
    isShowEditorCms = FacesContexts.evaluateValueExpression("#{data.showEditorCms}", Boolean.class);
    savedCmsMap = SavedCmsRepo.findAll();
    pmvCmsMap = new HashMap<>();
    for (var app : IApplicationRepository.of(ISecurityContext.current()).all()) {
      app.getProcessModels().stream().filter(processModel -> isActive(processModel))
          .map(IProcessModel::getReleasedProcessModelVersion)
          .filter(pmv -> isActive(pmv))
          .forEach(pmv -> getAllChildren(pmv.getName(), ContentManagement.cms(pmv).root(), new ArrayList<>()));
    }
    onAppChange();
  }

  public void writeCmsToApplication() {
    this.isEditableCms = false;
    CmsService.getInstance().writeCmsToApplication(this.savedCmsMap);
    onAppChange();
    PF.current().ajax().update(CONTENT_FORM);
    PrimeFaces.current().executeScript(OPEN_SUCCESS_DIALOG_SCRIPT);
  }

  public void onEditableButton() {
    this.isEditableCms = true;
  }

  public void onCancelEditableButton() {
    this.isEditableCms = false;
    this.lastSelectedCms.getContents().forEach(cms -> cms.setEditting(false));
    PF.current().ajax().update(CONTENT_FORM_LINK_COLUMN, CONTENT_FORM_EDITABLE_COLUMN);
  }

  public boolean isDisableEditableButton() {
    return ObjectUtils.isEmpty(this.selectedCms);
  }

  public void search() {
    if (isEditing()) {
      return;
    }
    filteredCMSList = cmsList.stream()
        .filter(entry -> isCmsMatchSearchKey(entry, searchKey))
        .map(entry -> CmsService.getInstance().compareWithCmsInApplication(entry))
        .collect(Collectors.toList());

    if (selectedCms != null) {
      selectedCms =
          filteredCMSList.stream().filter(entry -> entry.getUri().equals(selectedCms.getUri())).findAny().orElse(null);
    }
  }

  public void onAppChange() {
    if (StringUtils.isBlank(this.selectedProjectName)) {
      cmsList = this.pmvCmsMap.values().stream().map(PmvCms::getCmsList).flatMap(List::stream).toList();
    } else {
      cmsList = this.pmvCmsMap.values().stream().filter(pmvCms -> pmvCms.getPmvName().equals(this.selectedProjectName))
          .map(PmvCms::getCmsList).flatMap(List::stream).toList();
    }
    search();
  }

  public void rowSelect() {
    isEditableCms = false;
    if (isEditing()) {
      isEditableCms = true;
      selectedCms = lastSelectedCms; // Revert to last valid selection
    } else {
      PF.current().ajax().update(CONTENT_FORM_CMS_VALUES, CONTENT_FORM_SELECTED_URL, CONTENT_FORM_CMS_EDIT_VALUE,
          CONTENT_FORM_EDITABLE_COLUMN);
    }
  }
  
  public void saveAll() throws JsonMappingException, JsonProcessingException {
    var languageIndexAndContentJsonString =
        FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("values");
    List<CmsValueDto> cmsValues = mapper.readValue(languageIndexAndContentJsonString, new TypeReference<>() {});
    for (CmsValueDto currentCmsValue : cmsValues) {
      save(currentCmsValue.getLanguageIndex(), currentCmsValue.getContents());
    }
  }

  private boolean isEditing() {
    var isEditing = lastSelectedCms != null && lastSelectedCms.isEditing();
    if (isEditing) {
      showHaveNotBeenSavedDialog();
      PF.current().ajax().update(CONTENT_FORM_TABLE_CMS_KEYS);
    }
    return isEditing;
  }

  private void showHaveNotBeenSavedDialog() {
    var editingCmsList = lastSelectedCms.getContents().stream().filter(CmsContent::isEditting)
        .map(CmsContent::getLocale).map(Locale::getDisplayLanguage).collect(Collectors.toList());
    var detail = Utils.convertListToHTMLList(editingCmsList);
    showDialog(cms().co("/Labels/SomeFieldsHaveNotBeenSaved"), detail);
  }

  private void showDialog(String summary, String detail) {
    var message = new FacesMessage(SEVERITY_INFO, summary, detail);
    PrimeFaces.current().dialog().showMessageDynamic(message, false);
  }

  public void getAllChildren(String pmvName, ContentObject contentObject, List<Locale> locales) {
    // Exclude the CMS of it self
    if (!isShowEditorCms && Strings.CS.contains(pmvName, CMS_EDITOR_PMV_NAME)
        && !Strings.CS.contains(pmvName, CMS_EDITOR_DEMO_PMV_NAME)) {
      return;
    }

    if (contentObject.isRoot()) {
      locales =
          contentObject.cms().locales().stream().filter(locale -> isNotBlank(locale.getLanguage())).collect(toList());
    }

    for (ContentObject child : contentObject.children()) {
      if (child.children().size() == 0) {
        // just allow string cms. not file
        if (StringUtils.isBlank(child.meta().fileExtension())) {
          var cms = convertToCms(child, locales, pmvName);
          if (cms.getContents() != null) {
            var contents = this.pmvCmsMap.getOrDefault(pmvName, new PmvCms(pmvName, locales));
            contents.addCms(cms);
            this.pmvCmsMap.putIfAbsent(pmvName, contents);
          }
        }
      }
      getAllChildren(pmvName, child, locales);
    }
  }

  private Cms convertToCms(ContentObject contentObject, List<Locale> locales, String pmvName) {
    var cms = new Cms();
    cms.setUri(contentObject.uri());
    cms.setPmvName(pmvName);
    for (var i = 0; i < locales.size(); i++) {
      var locale = locales.get(i);
      var value = contentObject.value().get(locale);
      var valueString = ofNullable(value).map(ContentObjectValue::read).map(ContentObjectReader::string).orElse(EMPTY);
      var savedCms = findSavedCms(contentObject.uri(), locale);
      String originalValue = valueString;
      if (savedCms != null) {
        originalValue = savedCms.getOriginalContent();
        if (valueString.equals(savedCms.getOriginalContent())) {
          valueString = savedCms.getNewContent();
        } else {
          SavedCmsRepo.delete(savedCms);
        }
      }
      cms.addContent(new CmsContent(i, locale, originalValue, valueString));
    }
    return cms;
  }

  private static boolean isActive(IActivity processModelVersion) {
    return processModelVersion != null && ActivityState.ACTIVE == processModelVersion.getActivityState();
  }

  private boolean isCmsMatchSearchKey(Cms entry, String searchKey) {
    if (StringUtils.isNotBlank(searchKey)) {
      return Strings.CI.contains(entry.getUri(), searchKey)
          || entry.getContents().stream().anyMatch(value -> Strings.CI.contains(value.getContent(), searchKey));
    }
    return true;
  }

  private SavedCms findSavedCms(String uri, Locale locale) {
    return savedCmsMap.getOrDefault(uri, new HashMap<>()).getOrDefault(locale.toString(), null);
  }

  private void saveCms(SavedCms savedCms) {
    SavedCms savedCmsResult = SavedCmsRepo.save(savedCms);
    Map<String, SavedCms> cmsLocaleMap = savedCmsMap.computeIfAbsent(savedCmsResult.getUri(), key -> new HashMap<>());
    cmsLocaleMap.put(savedCmsResult.getLocale(), savedCmsResult);
  }

  public void save(int languageIndex, String content) {
    selectedCms.getContents().stream().filter(value -> value.getIndex() == languageIndex).findAny()
        .ifPresent(cmsContent -> handleCmsContentSave(content, cmsContent));
  }

  private void handleCmsContentSave(String newContent, CmsContent cmsContent) {
    cmsContent.saveContent(newContent);
    var locale = cmsContent.getLocale();
    var savedCms = findSavedCms(selectedCms.getUri(), locale);
    if (savedCms != null) {
      savedCms.setNewContent(cmsContent.getContent());
      savedCms.setOriginalContent(cmsContent.getOriginalContent());
    } else {
      savedCms = new SavedCms(selectedCms.getUri(), locale.toString(), cmsContent.getOriginalContent(),
          cmsContent.getContent());
    }
    saveCms(savedCms);
  }

  public void setValueChanged() {
    var context = FacesContext.getCurrentInstance();
    var requestParamMap = context.getExternalContext().getRequestParameterMap();
    var languageIndex = valueOf(requestParamMap.get("languageIndex"));
    selectedCms.getContents().get(languageIndex).setEditting(true);
  }

  public void handleBeforeDownloadFile() throws Exception {
    this.fileDownload = CmsFileUtils.writeCmsToZipStreamedContent(selectedProjectName, this.pmvCmsMap);
  }

  public void downloadFinished() {
    showDialog(cms().co("/Labels/Message"), cms().co("/Labels/CmsDownloaded"));
  }

  public String getActiveIndex() {
    return Optional.ofNullable(selectedCms).map(Cms::getContents).map(
        values -> IntStream.rangeClosed(0, values.size()).mapToObj(Integer::toString).collect(Collectors.joining(",")))
        .orElse(StringUtils.EMPTY);
  }

  public List<Cms> getFilteredCMSKeys() {
    return filteredCMSList;
  }

  public void setFilteredCMSKeys(List<Cms> filteredCMSKeys) {
    this.filteredCMSList = filteredCMSKeys;
  }

  public Cms getSelectedCms() {
    return selectedCms;
  }

  public void setSelectedCms(Cms selectedCms) {
    this.lastSelectedCms = this.selectedCms == null ? selectedCms : this.selectedCms;
    this.selectedCms = selectedCms;
  }

  public String getSearchKey() {
    return searchKey;
  }

  public void setSearchKey(String searchKey) {
    this.searchKey = searchKey;
  }

  public StreamedContent getFileDownload() {
    return fileDownload;
  }

  public String getSelectedProjectName() {
    return selectedProjectName;
  }

  public void setSelectedProjectName(String selectedProjectName) {
    this.selectedProjectName = selectedProjectName;
  }

  public boolean isEditableCms() {
    return isEditableCms;
  }

  public void setEditableCms(boolean isEditableCms) {
    this.isEditableCms = isEditableCms;
  }

  public Map<String, PmvCms> getPmvCmsMap() {
    return pmvCmsMap;
  }

  public void setPmvCmsMap(Map<String, PmvCms> pmvCmsMap) {
    this.pmvCmsMap = pmvCmsMap;
  }

  public Set<String> getProjectCms() {
    return IApplication.current().getProcessModels().stream()
        .filter(processModel -> isActive(processModel))
        .map(IProcessModel::getReleasedProcessModelVersion)
        .filter(pmv -> isActive(pmv))
        .map(pmv -> pmv.getProjectName())
        .filter(projectName -> this.pmvCmsMap.keySet().contains(projectName))
        .collect(Collectors.toSet());
  }

}
