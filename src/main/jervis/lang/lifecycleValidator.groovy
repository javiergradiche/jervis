package jervis.lang

import groovy.json.JsonSlurper
import jervis.exceptions.LifecycleBadValueInKeyException
import jervis.exceptions.LifecycleInfiniteLoopException
import jervis.exceptions.LifecycleMissingKeyException
import jervis.exceptions.LifecycleValidationException


//import jervis.lang.lifecycleValidator
//URL url = new URL('file:///home/sam/git/github/jervis/src/resources/lifecycles.json')
//def x = new lifecycleValidator()
//x.load_JSON(url)
//x.validate()

/**
  Validates the contents of a <a href="https://github.com/samrocketman/jervis/wiki/Specification-for-lifecycles-file" target="_blank">lifecycle file</a> and provides quick access to supported languages.

  <h2>Sample usage</h2>
<pre><tt>import jervis.lang.lifecycleValidator
import jervis.tools.scmGit
def git = new scmGit()
def lifecycles = new lifecycleValidator()
lifecycles.load_JSON(git.getRoot() + "/src/resources/lifecycles.json")
println "Does the file validate? " + lifecycles.validate()
println "Supported languages include:"
//print out a sorted ArrayList of supported languages
supported_languages = []
lifecycles.languages.each { supported_languages << lifecycles.lifecycles[it]["friendlyName"] }
Collections.sort(supported_languages)
supported_languages.each{ println it }</tt></pre>
 */
class lifecycleValidator {
    /**
      A <tt>{@link HashMap}</tt> of the parsed lifecycles file.
     */
    def lifecycles
    /**
      A <tt>String</tt> <tt>{@link Array}</tt> which contains a list of supported languages in the lifecycles file.  This is just a list of the keys.
     */
    def languages
    /**
      Load the JSON of a lifecycles file and parse it.  This should be the first function called upon class instantiation.  It populates <tt>{@link #lifecycles}</tt> and <tt>{@link #languages}</tt>.
      @param file A <tt>String</tt> which is a path to a lifecycles file.
     */
    public void load_JSON(String file) {
        lifecycles = new groovy.json.JsonSlurper().parse(new File(file).newReader())
        languages = lifecycles.keySet() as String[];
    }
    /**
      Checks to see if a language is a supported language based on the lifecycles file.
      @param lang A <tt>String</tt> which is a language to look up based on the keys in the lifecycles file.
      @return     <tt>true</tt> if the language is supported or <tt>false</tt> if the language is not supported.
     */
    public Boolean supportedLanguage(String lang) {
        lang in languages
    }
    /**
      Executes the <tt>{@link #validate()}</tt> function but always returns a <tt>Boolean</tt> instead of throwing an exception upon failed validation.
      @return     <tt>true</tt> if the lifecycles file validates or <tt>false</tt> if it fails validation.
     */
    public Boolean validate_asBool() {
        try {
            this.validate()
            return true
        }
        catch(LifecycleValidationException E) {
            return false
        }
    }
    /**
      Validates the lifecycles file.
      @return     <tt>true</tt> if the lifecycles file validates.  If the lifecycles file fails validation then it will throw a <tt>{@link jervis.exceptions.LifecycleValidationException}</tt>.
     */
    public Boolean validate() {
        lifecycles.keySet().each {
            def tools = lifecycles[it].keySet() as String[]
            if(!("defaultKey" in tools)) {
                throw new LifecycleMissingKeyException([it,"defaultKey"].join('.'))
            }
            if(!("friendlyName" in tools)) {
                throw new LifecycleMissingKeyException([it,"friendlyName"].join('.'))
            }
            if(!(lifecycles[it]["defaultKey"] in tools)) {
                throw new LifecycleMissingKeyException([it,"defaultKey",lifecycles[it]["defaultKey"]].join('.'))
            }
            def current_key = lifecycles[it]["defaultKey"]
            def count=0
            while(lifecycles[it][current_key] != null) {
                def cycles = lifecycles[it][current_key].keySet() as String[]
                if("fileExistsCondition" in cycles) {
                    //check for leading slash in the first element of fileExistsCondition
                    if(lifecycles[it][current_key]["fileExistsCondition"][0][0] != '/') {
                        throw new LifecycleBadValueInKeyException([it,current_key,"fileExistsCondition","[0]"].join('.') + " first element does not begin with a '/'.")
                    }
                }
                if("fallbackKey" in cycles) {
                    if(!(lifecycles[it][current_key]["fallbackKey"] in tools)) {
                        throw new LifecycleMissingKeyException([it,current_key,"fallbackKey",lifecycles[it][current_key]["fallbackKey"]].join('.'))
                    }
                    if(!("fileExistsCondition" in cycles)) {
                        throw new LifecycleMissingKeyException([it,current_key,"fileExistsCondition"].join('.') + " required by " + [it,current_key,"fallbackKey"].join('.'))
                    }
                }
                count++
                if(count > 1000) {
                    throw new LifecycleInfiniteLoopException([it,current_key].join('.'))
                }
                current_key = lifecycles[it][current_key]["fallbackKey"]
            }
        }
        return true
    }
}
