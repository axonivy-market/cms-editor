function initSunEditor(isFormatButtonListVisible, languageIndex, editorId) {
  let buttonList;
  buttonList = [
    ['font'],
    ['bold', 'underline', 'italic'],
    ['fontColor','align', 'list'],
    ['fullScreen', 'save']
  ];
  const editor = SUNEDITOR.create(document.getElementById(editorId), {
    buttonList: buttonList,
    attributesWhitelist: {
      'all': 'style|width|height|role|border|cellspacing|cellpadding|src|alt|href|target'
    },
  });

  editor.onChange = (contents, core) => {
    setValueChanged([{
      name: 'languageIndex',
      value: languageIndex
    }]);
  };



  editor.onSave = (contents, core) => {
    if(removeNonPrintableChars(core.functions.getText()).trim().length === 0){
      core.functions.noticeOpen("The content must not be empty.");
      return;
    }
    saveValue([{
      name: 'contents',
      value: contents
    }, {
      name: 'languageIndex',
      value: languageIndex
    }]);
  };
};

function removeNonPrintableChars(str) {
  return str.replace(/[\u00A0\u0000\u200B]/g, '');
}

function bindCmsWarning(hoverId, warningId) {
  const hoverElement = document.getElementById(hoverId);
  const targetElement = document.getElementById(warningId);
  if (!hoverElement || !targetElement) return;
  let hideTimeout;
  function showWarning() {
    clearTimeout(hideTimeout);
    targetElement.style.display = "block";
  }
  function hideWarning() {
    hideTimeout = setTimeout(function () {
      targetElement.style.display = "none";
    }, 500);
  }
  hoverElement.addEventListener("mouseenter", showWarning);
  hoverElement.addEventListener("mouseleave", hideWarning);
  targetElement.addEventListener("mouseenter", function () {
    clearTimeout(hideTimeout);
  });
  targetElement.addEventListener("mouseleave", hideWarning);
}

function initCmsWarnings() {
  bindCmsWarning("content-form:download-button", "content-form:cms-warning-container");
  bindCmsWarning("content-form:save-button", "content-form:cms-warning-save-container");
}


function showDialog(dialogId) {
  PF(dialogId).show();
  setTimeout(function () {
    PF(dialogId).hide();
  }, 1500);
}

window.isHideTaskName = false;
window.isHideTaskAction = true;
window.isHideCaseInfo = true;
window.isWorkingOnATask = false;
document.addEventListener("DOMContentLoaded", initCmsWarnings);