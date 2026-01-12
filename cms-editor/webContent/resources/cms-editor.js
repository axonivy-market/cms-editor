window.cmsEditors = window.cmsEditors || {};

function initSunEditor(isFormatButtonListVisible, languageIndex, editorId) {

  // ❌ BỎ save khỏi toolbar
  const buttonList = [
    ['font'],
    ['bold', 'underline', 'italic'],
    ['fontColor', 'align', 'list'],
    ['fullScreen']
  ];

  const editor = SUNEDITOR.create(document.getElementById(editorId), {
    buttonList: buttonList,
    attributesWhitelist: {
      'all': 'style|width|height|role|border|cellspacing|cellpadding|src|alt|href|target'
    }
  });

  // lưu editor theo languageIndex
  window.cmsEditors[languageIndex] = editor;

  // đánh dấu content đã thay đổi
  editor.onChange = () => {
    setValueChanged([{
      name: 'languageIndex',
      value: languageIndex
    }]);
  };
}

/**
 * ✅ SAVE TẤT CẢ EDITOR 1 LẦN
 */
function saveAllEditors() {
  const values = [];

  for (const languageIndex in window.cmsEditors) {
    const editor = window.cmsEditors[languageIndex];

    const contents = editor.getContents();
    const text = removeNonPrintableChars(editor.getText()).trim();

    // validate rỗng
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

  // gọi remoteCommand saveAllValue
  saveAllValue([{
    name: 'values',
    value: JSON.stringify(values)
  }]);
}

/* ===================== UTILITIES ===================== */

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

/* ===================== GLOBAL FLAGS ===================== */

window.isHideTaskName = false;
window.isHideTaskAction = true;
window.isHideCaseInfo = true;
window.isWorkingOnATask = false;

document.addEventListener("DOMContentLoaded", initCmsWarnings);
