package com.axonivy.utils.cmseditor.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;

import com.axonivy.utils.cmseditor.model.SavedCms;

import ch.ivyteam.ivy.application.ActivityState;
import ch.ivyteam.ivy.application.IApplication;
import ch.ivyteam.ivy.application.IProcessModel;
import ch.ivyteam.ivy.application.app.IApplicationRepository;
import ch.ivyteam.ivy.cm.ContentManagementSystem;
import ch.ivyteam.ivy.cm.ContentObject;
import ch.ivyteam.ivy.cm.exec.ContentManagement;
import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.security.ISecurityContext;
import ch.ivyteam.ivy.security.exec.Sudo;

public class CmsService {
  private static CmsService instance;
  private static List<IProcessModel> activeProcessModels;
  
  public static CmsService getInstance() {
    if (instance == null) {
      instance = new CmsService();
    }
    return instance;
  }

  public void updateTranslation(String processModelName) {
    
//    ContentObject co=  ContentManagement.cms(IApplication.current()).get("/com/axonivy/utils/cmseditor/demo/TODO")
//        .orElse(ContentManagement.cms(IApplication.current()).root().child().string("/com/axonivy/utils/cmseditor/demo/TODO"));
//    
//    co.value().get(Locale.ENGLISH).write().string("Phuc khoai to");
    Sudo.run(() -> {
      IProcessModel processModel = getProcessModel(processModelName);
      IApplication currentApplication = IApplication.current();
      if (processModel == null || currentApplication == null) {
        Ivy.log().warn("Cannot find the processModel or current application");
        return;
      }
      ContentManagementSystem processModelCMS = ContentManagement.cms(processModel.getReleasedProcessModelVersion());
      var supportLocale = processModelCMS.locales();
      var rootContentObject = processModelCMS.root();
      for (var contentObject : rootContentObject.children()) {
        if (contentObject.children().isEmpty()) {
          writeDataToCMS(supportLocale, currentApplication, contentObject);
        } else {
          proceedToChildCMS(supportLocale, currentApplication, contentObject);
        }
      }
    });
  }
  
  private List<IProcessModel> getActiveProcessModels() {
    if (CollectionUtils.isEmpty(activeProcessModels)) {
      activeProcessModels = IApplicationRepository.of(ISecurityContext.current()).all()
          .stream().map(IApplication::getProcessModelsSortedByName)
          .flatMap(List::stream).filter(pm -> pm.getActivityState() == ActivityState.ACTIVE).toList();
    }
    return activeProcessModels;
  }
  
  private void proceedToChildCMS(Set<Locale> supportLocale, IApplication portalApp, ContentObject contentObject) {
    for (var childContentObject : contentObject.children()) {
      if (childContentObject.children().isEmpty()) {
        writeDataToCMS(supportLocale, portalApp, childContentObject);
      } else {
        proceedToChildCMS(supportLocale, portalApp, childContentObject);
      }
    }
  }
  
  private void writeDataToCMS(Set<Locale> supportLocale, IApplication application, ContentObject contentObject) {
    var portalCMSEntity = createOrGetCMSByURI(application, contentObject);
    for (var locale : CollectionUtils.emptyIfNull(supportLocale)) {
      var cmsAsStringValue = contentObject.value().get(locale).read().string();
      portalCMSEntity.value().get(locale).write().string(cmsAsStringValue);
    }
  }
  
  private ContentObject createOrGetCMSByURI(IApplication application, ContentObject cms) {
    var portalCMSEntity = ContentManagement.cms(application).get(cms.uri());
    if (portalCMSEntity.isEmpty()) {
      return ContentManagement.cms(application).root().child().string(cms.uri());
    }
    return portalCMSEntity.get();
  }
  
  private IProcessModel getProcessModel(String processModelName) {
    return getActiveProcessModels().stream().filter(pm -> processModelName.equals(pm.getName())).findFirst()
        .orElse(null);
  }
  
  private ContentObject createOrGetCMSByURI( String uri) {
    IApplication currentApplication = IApplication.current();
    var portalCMSEntity = ContentManagement.cms(currentApplication).get(uri);
    if (portalCMSEntity.isEmpty()) {
      return ContentManagement.cms(currentApplication).root().child().string(uri);
    }
    return portalCMSEntity.get();
  }
  

  public void writeCmsToApplication(Map<String, Map<String, SavedCms>> savedCmsMap) {
    savedCmsMap.forEach((uri, localeAndContent) -> {
      ContentObject currentContentObject = createOrGetCMSByURI(uri);
      localeAndContent.forEach((locale, savedCms) -> {
        currentContentObject.value().get(Locale.forLanguageTag(locale)).write().string(savedCms.getNewContent());
      });
    });
  }
}
