window.cmsEditors = window.cmsEditors || {};
window.cmsDirtyEditors = new Set();

function initSunEditor(isFormatButtonListVisible, languageIndex, editorId) {
  let buttonList;
  if (isFormatButtonListVisible) {
    buttonList = [
      ['font', 'fontSize', 'formatBlock'],
      ['paragraphStyle', 'blockquote'],
      ['bold', 'underline', 'italic', 'strike', 'subscript', 'superscript'],
      ['fontColor', 'hiliteColor', 'textStyle'],
      ['removeFormat'],
      ['outdent', 'indent'],
      ['align', 'list', 'lineHeight', 'horizontalRule'],
      ['table', 'link'],
      ['fullScreen'],
      ['undo', 'redo']
    ];
  } else {
    buttonList = [
      ['font'],
      ['bold', 'underline', 'italic'],
      ['fontColor', 'align', 'list'],
      ['fullScreen']
    ];
  }

  const editor = SUNEDITOR.create(document.getElementById(editorId), {
    buttonList: buttonList,
    attributesWhitelist: {
      'all': 'style|width|height|role|border|cellspacing|cellpadding|src|alt|href|target'
    }
  });

  window.cmsEditors[languageIndex] = editor;

  editor.onChange = () => {
    window.cmsDirtyEditors.add(languageIndex);
    setValueChanged([{
      name: 'languageIndex',
      value: languageIndex
    }]);
  };
}

function saveAllEditors() {
  console.log(window.cmsDirtyEditors);
  const values = [];

  for (const languageIndex of window.cmsDirtyEditors) {
    const editor = window.cmsEditors[languageIndex];

    const contents = editor.getContents();
    const text = removeNonPrintableChars(editor.getText()).trim();

    if (text.length === 0) {
      editor.noticeOpen("The content must not be empty.");
      return;
    }

    values.push({
      languageIndex: Number(languageIndex),
      contents: contents
    });
  }

  console.log(values)

  saveAllValue([{
    name: 'values',
    value: JSON.stringify(values)
  }]);
}


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
    hideTimeout = setTimeout(function() {
      targetElement.style.display = "none";
    }, 500);
  }

  hoverElement.addEventListener("mouseenter", showWarning);
  hoverElement.addEventListener("mouseleave", hideWarning);
  targetElement.addEventListener("mouseenter", function() {
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
  setTimeout(function() {
    PF(dialogId).hide();
  }, 1500);
}

window.isHideTaskName = false;
window.isHideTaskAction = true;
window.isHideCaseInfo = true;
window.isWorkingOnATask = false;

document.addEventListener("DOMContentLoaded", initCmsWarnings);