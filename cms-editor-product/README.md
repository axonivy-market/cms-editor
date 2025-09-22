# CMS Editor
In AxonIvy, languages for UIs, notifications, or emails are managed within the CMS. We are excited to introduce the new CMS editor that significantly simplifies language editing! The key features are:

- User-friendly editor for translating new languages
- Edit an unlimited number of languages
- Simple styles available
- No HTML tags needed in the translation text

** *In this version, real-time CMS updates have not yet been supported. Users are required to download the modified bundle in a ZIP format after making edits and re-import it into the project using Axon Ivy Designer. The changes will take effect only after the new release package is created and deployed to engine.* **

## Demo
### Starting the CMS Editor process
![](./images/1-cms-editor-process.png)
- Users must have the role of "CMS_ADMIN" to start the process.


### How-To
![](./images/2-cms-editor-main-page.png)

**1 Application Selector:** An Ivy Engine can host multiple applications. First, select the application you want to work with.  
**2 Search:** Enter text to search by URI or content. The search is case-insensitive.  
**3 Selected Key:** Displays the key of the selected content.  
**4 Filter Only TODO:** This option filters all content with the prefix 'TODO'.  
**5 URI Table: Displays all URIs.   
**6 Selecting Key:** Clicking on the URI loads the content into the content area.  
**7 Content Area:** Shows all content in different languages in preview mode.  
**8 Language Name**  
**9. Content**  
**10 WYSIWYG Editor:** Displays the content in edit mode. You can update the content using the WYSIWYG editor.    
**11. Save Button:** The editor does not save automatically. Click the save button to save the edited content.  
**12. Cancel Button:**  
**13. Download Button:** Downloads a zip file for deployment.  

