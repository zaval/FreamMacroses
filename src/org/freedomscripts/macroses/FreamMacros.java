package org.freedomscripts.macroses;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.editor.actionSystem.EditorWriteActionHandler;
import com.intellij.openapi.util.TextRange;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by zaval on 08.12.14.
 */
public class FreamMacros extends EditorAction {

    public FreamMacros(EditorActionHandler defaultHandler) {
        super(defaultHandler);
    }

    public  FreamMacros()
    {
        this(new MacroHandler());
    }


    private static  class MacroHandler extends EditorWriteActionHandler{
        private MacroHandler() {
        }

        @Override
        public void executeWriteAction(Editor editor, DataContext dataContext) {
            Document document = editor.getDocument();

            if (editor == null || document == null || !document.isWritable()) {
                return;
            }

            // CaretModel used to find caret position
            CaretModel caretModel = editor.getCaretModel();
            // SelectionModel used to find selection ranges
            SelectionModel selectionModel = editor.getSelectionModel();

            // get the range of the selected characters
            TextRange charsRange = new TextRange(selectionModel.getSelectionStart(), selectionModel.getSelectionEnd());
            // get the range of the selected lines (block of code)
            TextRange linesRange = new TextRange(document.getLineNumber(charsRange.getStartOffset()), document.getLineNumber(charsRange.getEndOffset()));
            // range of the duplicated string
            TextRange linesBlock = new TextRange(document.getLineStartOffset(linesRange.getStartOffset()), document.getLineEndOffset(linesRange.getEndOffset()));

            // get the string to duplicate
//            String macrosString = document.getText().substring(linesBlock.getStartOffset(), linesBlock.getEndOffset());
            String macrosString = document.getText().substring(charsRange.getStartOffset(), charsRange.getEndOffset());
            String directSelection = document.getText().substring(charsRange.getStartOffset(), charsRange.getEndOffset());

            String newStr = macrosString;

            int cursorFixer = 0;
            boolean dontReplaceAll = false;

            if (macrosString.matches("^\\s*http\\s\\S+$"))
            {


                Pattern pattern = Pattern.compile("^(\\s*)http\\s+(\\S+)$");
                Matcher matcher = pattern.matcher(macrosString);

                if (matcher.matches()) {
                    if (matcher.group(2).startsWith("http"))
                        newStr = matcher.group(1) + "page = self.http.get('" + matcher.group(2) + "')";
                    else
                        newStr = matcher.group(1) + "page = self.http.get(" + matcher.group(2) + ")";

                }
            }

            else if (macrosString.matches("^f$")){
                    newStr =  ".format()";
                    cursorFixer = -1;

            }

            else if (macrosString.matches("^\\s*http\\s\\S+\\s\\S+$"))
            {
                Pattern pattern = Pattern.compile("^(\\s*)http\\s+(\\S+)\\s(\\S+)");
                Matcher matcher = pattern.matcher(macrosString);

                if (matcher.matches()) {
                    String variable = new String();

                    if (matcher.group(2).startsWith("http"))
                        variable = "'" + matcher.group(2) + "'";
                    else
                        variable = matcher.group(2);

                    if (matcher.group(3).equals("ajax"))
                        newStr = matcher.group(1) + "page = self.http.ajax().post(" + variable + ", data)";
                    else
                        newStr = matcher.group(1) + "page = self.http.post(" + variable + ", data)";
                }
            }

            else if (macrosString.matches("^\\s*pd\\s(.+)$"))
            {
                Pattern pattern = Pattern.compile("^(\\s*)pd\\s+(.+)$");
                Matcher matcher = pattern.matcher(macrosString);

                if (matcher.matches()){
                    newStr = matcher.group(1) + "data = {\n";

                    newStr += makePostData(matcher.group(2), matcher.group(1));

                    newStr += matcher.group(1) + "}\n";

                }
            }

            else if (macrosString.matches("^\\s*pdf\\s([\\S\\s]+)$"))
            {
                String prefix = new String();

                Pattern pattern = Pattern.compile("^(\\s*)pdf\\s([\\s\\S]+)$");
                Matcher matcher = pattern.matcher(macrosString);
                if (matcher.matches())
                {
                    System.out.println("'" + matcher.group(1) + "'");
                    prefix = matcher.group(1);
                }

                System.out.println("'" + prefix + "'");

                String resString = new String();

                Matcher m = Pattern.compile("name=\"([^\"]+)\"\\n\\n\\s*([^\\n]+)").matcher(macrosString);

                while (m.find())
                {
                    resString += m.group(1) + "=" + m.group(2) + "&";
                }

                m = Pattern.compile("name=\"([^\"]+)\"; filename=").matcher(macrosString);

                while (m.find())
                {
                    resString += m.group(1) + "=@{}&";
                }

                newStr = prefix + "data = {\n";
                newStr += makePostData(resString, prefix);
                newStr += prefix + "}\n";

            }

            else if (macrosString.matches("^\\s*(?:parse|parse_all)\\s\\S+"))
            {
                Pattern pattern = Pattern.compile("^(\\s*)(parse|parse_all)\\s+(\\S+)$");
                Matcher matcher = pattern.matcher(macrosString);

                if (matcher.matches()){
                    newStr = matcher.group(1) + matcher.group(3) + " = hlp." + matcher.group(2) + "(r'([^\"]+)', page)\n" + matcher.group(1) + "if " + matcher.group(3) + " is False:\n" + matcher.group(1) + "\tself.log('не смогли достать " + matcher.group(3) + "', '!')\n" + matcher.group(1) + "\treturn False\n" + matcher.group(1) + "else:\n" + matcher.group(1) + "\tself.log('достали " + matcher.group(3) + ": {}'.format(" + matcher.group(3) + "), '+')";

                    int index = newStr.indexOf("([^");
                    cursorFixer = index - newStr.length();

                }
            }

            else if (macrosString.matches("^\\s*l\\s.+?\\s(?:\\+|\\*|\\-|~|!)")){
                Pattern pattern = Pattern.compile("^(\\s*)l\\s(.+?)\\s*(\\+|\\*|\\-|~|!)$");
                Matcher matcher = pattern.matcher(macrosString);

                if (matcher.matches()){
                    if (matcher.group(2).matches("^[a-zA-Z_][a-zA-Z_0-9]+$"))
                        newStr = matcher.group(1) + "self.log(" + matcher.group(2) + ", '" + matcher.group(3) + "')";
                    else
                        newStr = matcher.group(1) + "self.log('" + matcher.group(2) + "', '" + matcher.group(3) + "')";
                }

            }


            else if (macrosString.matches("^\\s*l\\s.+")){
                Pattern pattern = Pattern.compile("^(\\s*)l\\s(.+)$");
                Matcher matcher = pattern.matcher(macrosString);

                if (matcher.matches()){
                    if (matcher.group(2).matches("^[a-zA-Z_][a-zA-Z_0-9]+$"))
                        newStr = matcher.group(1) + "self.log(" + matcher.group(2) + ")";
                    else
                        newStr = matcher.group(1) + "self.log('" + matcher.group(2) + "')";
                }

            }

            else if (macrosString.matches("^\\s*cnf\\s(\\S+)\\s+(?:int|bool|num|list)")){
                Pattern pattern = Pattern.compile("^(\\s*)cnf\\s(\\S+)\\s+(int|bool|num|list)");
                Matcher matcher = pattern.matcher(macrosString);

                if (matcher.matches()){
                    if (matcher.group(3).equals("int"))
                        newStr = matcher.group(1) + "self.cnf.getint('" + matcher.group(2) + "')";

                    if (matcher.group(3).equals("num"))
                        newStr = matcher.group(1) + "self.cnf.getnum('" + matcher.group(2) + "')";

                    if (matcher.group(3).equals("bool"))
                        newStr = matcher.group(1) + "self.cnf.getbool('" + matcher.group(2) + "')";

                    if (matcher.group(3).equals("list"))
                        newStr = matcher.group(1) + "self.cnf.getlist('" + matcher.group(2) + "')";

                }

            }

            else if (macrosString.matches("^\\s*cnf\\s(\\S+)")){
                Pattern pattern = Pattern.compile("^(\\s*)cnf\\s(\\S+)");
                Matcher matcher = pattern.matcher(macrosString);

                if (matcher.matches()){
                    newStr = matcher.group(1) + "self.cnf.get('" + matcher.group(2) + "')";
                }

            }

            else if (macrosString.matches("^\\s*cnt\\s(\\S+)")){
                Pattern pattern = Pattern.compile("^(\\s*)cnt\\s(\\S+)");
                Matcher matcher = pattern.matcher(macrosString);

                if (matcher.matches()){
                    newStr = matcher.group(1) + "self.hlp.cnt('" + matcher.group(2) + "')";

                }

            }

            else if (macrosString.matches("^\\s*ac$"))
            {
                Pattern pattern = Pattern.compile("^(\\s*)ac$");
                Matcher matcher = pattern.matcher(macrosString);
                if (matcher.matches()) {
                    newStr = matcher.group(1) + "actext = self.ac.captcha(captcha_url)\n" + matcher.group(1) + "if self.ac.error:\n" + matcher.group(1) + "\tself.log('ошибка разгадки: {}'.format(self.ac.last_error), '!')\n" + matcher.group(1) + "\treturn False\n" + matcher.group(1) + "else:\n" + matcher.group(1) + "\tself.log('разгадали: ' + actext, '+') # self.ac.recaptcha_challenge # self.ac.bad()";
                }
            }

            else if (macrosString.matches("^\\s*slp\\s+\\S+$"))
            {
                Pattern pattern = Pattern.compile("^(\\s*)slp\\s+(\\S+)$");
                Matcher matcher = pattern.matcher(macrosString);

                if (matcher.matches())
                {
                    try{
                        newStr = matcher.group(1) + "self.hlp.slp(" + Integer.parseInt(matcher.group(2)) + ", log=self.log)";
                    }
                    catch (NumberFormatException e)
                    {
                        newStr = matcher.group(1) + "self.hlp.slp('" + matcher.group(2) + "', log=self.log)";
                    }
                }
            }

            else if (macrosString.matches("^\\s*def\\s+\\S+$"))
            {
                Pattern pattern = Pattern.compile("^(\\s*)def\\s+(\\S+)$");
                Matcher matcher = pattern.matcher(macrosString);

                if (matcher.matches())
                {
                    newStr = matcher.group(1) + "def " + matcher.group(2) + "(self):\n" + matcher.group(1) + "\t'''\n" + matcher.group(1) + "\t\n" + matcher.group(1) + "\t@param\n" + matcher.group(1) + "\t@return\n" + matcher.group(1) + "\t'''\n" + matcher.group(1) + "\tself.log('')\n" + matcher.group(1) + "\t";
                }
            }

            else if (macrosString.matches("^data\\s+\\S+$"))
            {
                Pattern pattern = Pattern.compile("^data\\s+(\\S+)$");
                Matcher matcher = pattern.matcher(macrosString);

                if (matcher.matches())
                {
                    newStr = "self.data['" + matcher.group(1) + "']";
                }
            }

            else if (macrosString.matches("^b$"))
            {

                newStr = "<b>{}</b>";

                cursorFixer  = -5;
            }

            else if (macrosString.matches("^\\s*rand\\s+\\S+$"))
            {
                Pattern pattern = Pattern.compile("^(\\s*)rand\\s+(\\S+)$");
                Matcher matcher = pattern.matcher(macrosString);
                if (matcher.matches())
                {
                    newStr = matcher.group(1) + matcher.group(2) + " = hlp.rand(" + matcher.group(2) + ")";
                }
            }

            else if (macrosString.matches("^\\s*randomize\\s+\\S+$"))
            {
                Pattern pattern = Pattern.compile("^(\\s*)randomize\\s+(\\S+)$");
                Matcher matcher = pattern.matcher(macrosString);
                if (matcher.matches())
                {
                    newStr = matcher.group(1) + matcher.group(2) + " = hlp.randomize(" + matcher.group(2) + ")";
                }
            }

            else if (macrosString.matches("^ap$"))
            {
                newStr = "self.parent.active_project";
            }

            else if (macrosString.matches("^ap\\+$"))
            {
                newStr = "'projects/{}/'.format(self.parent.active_project)";

                int index = newStr.indexOf("'.format");
                cursorFixer = index - newStr.length();
            }

            if (!dontReplaceAll)
                document.replaceString(charsRange.getStartOffset(), charsRange.getEndOffset(), newStr);
            caretModel.moveToOffset(charsRange.getStartOffset() + newStr.length() + cursorFixer);
            editor.getSelectionModel().removeSelection();
            editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
        }

        private String makePostData(String data, String prefix){

            String newStr = new String();
            String[] datavalues = data.split("&");

            for (int i = 0; i < datavalues.length; i++){
                String[] kv = datavalues[i].split("=");

                if (kv.length == 2){
                    try {
                        newStr += prefix  + "\t'" + URLDecoder.decode(kv[0], "utf-8") + "': '" + URLDecoder.decode(kv[1], "utf-8") + "',\n";
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
                else
                {
                    try {
                        newStr += prefix + "\t'" + URLDecoder.decode(kv[0], "utf-8") + "': '',\n";
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
            }

            return newStr;
        }
    }
}
