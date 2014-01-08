(ns clj-mybatis-mapper.core
  (:require [clojure.string :as str]
            [clojure.java.jdbc :as jdbc]
            [clojure.java.io :as io])
  (:use [clojure.tools.cli :only [cli]])
  (:gen-class))

(defn ->caml
  "transform to caml case, for example: user_id -> userId"
  [text]
  (let [[first & more] (str/split text #"_")]
    (str/join "" (cons (str/lower-case first) (map str/capitalize more)))))

(defn ->pascal
  "transform to pascal case, for example: user_id -> UserId"
  [text]
  (str/join "" (map str/capitalize (str/split text #"_"))))

(defn- some-start-with
  "Returns true when set has element startWith text"
  [sets text]
  (some #(.startsWith text %) sets))

(defn ->java-type
  "transform jdbc type to java type, for example: varchar(10) -> String"
  [text]
  (condp some-start-with text
    #{"int" "short" "enum"} "Integer"
    #{"varchar char"} "String"
    #{"decimal long"} "Long"
    "String"))

(defn get-table-colunm
  "Gets raw table column from db"
  [table-name]
  (jdbc/with-query-results res [(str "show full fields from " table-name)]
    (doall (map (fn [e] {:field (:field e),
                         :type (:type e),
                         :comment (:comment e)}) res))))

(defn get-table-info-in-db
  "Gets raw table info from db"
  []
  (jdbc/with-query-results res ["show table status"]
    (doall (map (fn [e] {:name (:name e)
                         :comment (:comment e)
                         :columns []}) res))))

(defn get-table-detail-in-db
  "Gets raw table detail in db"
  []
  (doall (map (fn [e] (let [name (:name e)]
                        (assoc e :columns (get-table-colunm name) :name name)))
              (get-table-info-in-db))))

(defn- get-output-file-path 
  "Gets output file path"
  [output-folder file-name]
  (if output-folder
    (do 
      (.mkdir (io/file output-folder))
      (str output-folder "/" file-name))
    file-name))

(defn ->java-files 
  "Outputs the java file for table schema"
  [tables pack output-folder]
  (io!
    (doseq [table tables]
      (let [pascal-name (->pascal (:name table))]
        (with-open [w (io/writer (get-output-file-path output-folder (str pascal-name ".java")) :append false)]
          (.write w "/*\n")
          (.write w " * $Id$\n")
          (.write w " *\n")
          (.write w " * Copyright (c) 2012 xxx. All Rights Reserved.\n")
          (.write w " */\n")
          (if pack
            (.write w (str "package " pack ";\n")))
          (.write w "\n")
          (.write w "/**\n")
          (.write w (str " *" (:comment table) "\n"))
          (.write w " */\n")
          (.write w (str "public class " pascal-name " {\n"))
          (.write w "\n")
          (doseq [column (:columns table)]
            (do
              (.write w "\n")
              (if-let [comment (:comment column)]
                (do
                  (.write w "    /**\n")
                  (.write w (str "     * " comment "\n"))
                  (.write w "     */\n")))
              (.write w (str "    private " (->java-type (:type column)) " " (->caml (:field column)) ";\n"))))
          (doseq [column (:columns table)]
            (let [comment (:comment column)
                  field-type (->java-type (:type column))
                  pascal-field (->pascal (:field column))
                  caml-field (->caml (:field column))]
              (.write w "\n")
              (if comment
                (do 
                  (.write w "    /**\n")
                  (.write w (str "     * 获取" comment "\n"))
                  (.write w (str "     * \n"))
                  (.write w (str "     * @return " comment "\n"))
                  (.write w "     */\n")))
              (.write w (str "    public " field-type " get" pascal-field "() {\n"))
              (.write w (str "        return " caml-field ";\n"))
              (.write w "    }\n")
              (.write w "\n")
              (if comment
                (do
                  (.write w "    /**\n")
                  (.write w (str "     * 设置" comment "\n"))
                  (.write w "     * \n")
                  (.write w (str "     * @param " caml-field " " comment "\n"))
                  (.write w "     */\n")))
              (.write w (str "   public void set" pascal-field  "(" field-type " " caml-field ") {\n"))
              (.write w (str "        this." caml-field " = " pascal-field ";\n"))
              (.write w "    }\n")))
          (.write w "\n")
          (.write w "}\n'"))))))

(defn- concat-name [pack class-name]
  (if pack
    (str pack "." class-name)
    class-name))

(defn ->mapper-files 
  "Outputs the mybatis xml mapper files for table schema"
  [tables pack output-folder]
  (io!
    (doseq [table tables]
      (let [table-name (:name table)
            class-name (concat-name pack (->pascal table-name))
            caml-name (->caml table-name)
            [last-item & temp-but-latst] (reverse (:columns table))
            but-last (reverse temp-but-latst)]
        (with-open [w (io/writer (get-output-file-path output-folder (str caml-name ".xml")) :append false)]
          (.write w "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n")
          (.write w "<!DOCTYPE mapper\n")
          (.write w "        PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\"\n")
          (.write w "        \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\">\n")
          (.write w (str "<mapper namespace=\"" caml-name "\">\n"))
          (.write w "\n")
          (.write w "    <select id=\"get\"\n")
          (.write w "        parameterType=\"map\"\n")
          (.write w (str "        resultType=\"" class-name "\">\n"))
          (.write w "        <![CDATA[\n")
          (.write w "            select\n")
          (doseq [column but-last]
            (.write w (str "              `" (:field column) "` as " (->caml (:field column))  ",\n")))
          (.write w (str "              `" (:field last-item) "` as " (->caml (:field last-item))  "\n"))
          (.write w "            from \n")
          (.write w (str "                `" (:name table) "`\n"))
          (.write w "        ]]>\n")
          (.write w "        <where>\n")
          (doseq [column (:columns table)]
            (.write w (str "            <if test=\"" (->caml (:field column)) " != null and " (->caml (:field column)) " != ''\">\n"))
            (.write w (str "                and `" (:field column) "` = #{" (->caml (:field column)) "}\n"))
            (.write w "            </if>\n"))
          (.write w "        </where>\n")
          (.write w "    </select>\n")
          (.write w "\n")
          (.write w "    <insert id=\"insert\"\n")
          (.write w (str "        parameterType=\"" class-name "\">\n"))
          (.write w "        <![CDATA[\n")
          (.write w (str "        insert into `" (:name table) "` (\n"))
          (doseq [column but-last]
            (.write w (str "            `" (:field column) "`,\n")))
          (.write w (str "            `" (:field last-item) "`\n"))
          (.write w "        ) values (\n")
          (doseq [column but-last]
            (.write w (str "            #{" (->caml (:field column)) "},\n")))
          (.write w (str "            #{" (->caml (:field last-item)) "}\n"))
          (.write w "        )\n")
          (.write w "        ]]>\n")
          (.write w "    </insert>\n")
          (.write w "\n")
          (.write w "    <update id=\"update\"\n")
          (.write w "        parameterType=\"map\">\n")
          (.write w "        <![CDATA[\n")
          (.write w "        update\n")
          (.write w (str "            `"  (:name table) "`\n"))
          (.write w "        set\n")
          (doseq [column but-last]
            (.write w (str "            `"  (:field column)  "` = #{"  (->caml (:field column)) "},\n")))
          (.write w (str "            `"  (:field last-item)  "` = #{"  (->caml (:field last-item)) "}\n"))
          (.write w "        where\n")
          (.write w "            `id` = #{id, jdbcType=INTEGER}\n")
          (.write w "        ]]>\n")
          (.write w "    </update>\n")
          (.write w "</mapper>\n"))))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [[options extra-args banner]
        (cli args 
             ["-h" "--host" "Host for mysql server"]
             ["-P" "--port" "Port for mysql server" :default "3306" :parse-fn #(Integer. %)]
             ["-p" "--password" "Password for mysql" :default ""]
             ["-u" "--user" "User for mysql"]
             ["-pk" "--package" "Package for model" :default nil]
             ["-o" "--output" "Output folder for gen-file" :default nil]
             ["-db" "--db" "Database for gen"]
             ["-help" "--help" :default false :flag true])
        db-spec {:subprotocol "mysql"
                 :subname (str "//" (:host options) ":" (:port options) "/" (:db options))
                 :user (:user options)
                 :password (:password options)}
        pack (:package options)
        output-folder (:output options)]
    (when (:help options)
      (println banner)
      (System/exit 0))    
    (jdbc/with-connection db-spec
      (let [data (get-table-detail-in-db)]
        (->mapper-files data pack output-folder)
        (->java-files data pack output-folder)))))