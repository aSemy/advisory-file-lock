package process

/**
 * Provides access to information about the current process.
 *
 *
 * Implementations are not thread-safe.
 */
interface ProcessEnvironment {
  //    /**
  //     * Sets the environment of this process, if possible.
  //     *
  //     * @param source The environment
  //     * @return true if environment changed, false if not possible.
  //     */
  //    EnvironmentModificationResult maybeSetEnvironment(Map<String, String> source);
  //
  //    /**
  //     * Removes the given environment variable.
  //     *
  //     * @param name The name of the environment variable.
  //     * @throws NativeIntegrationException If the environment variable cannot be removed.
  //     */
  //    void removeEnvironmentVariable(String name) throws NativeIntegrationException;
  //
  //    /**
  //     * Removes the given environment variable, if possible.
  //     *
  //     * @param name The name of the environment variable.
  //     * @return true if removed, false if not possible.
  //     */
  //    EnvironmentModificationResult maybeRemoveEnvironmentVariable(String name);
  //
  //    /**
  //     * Sets the given environment variable.
  //     *
  //     * @param name The name
  //     * @param value The value. Can be null, which removes the environment variable.
  //     * @throws NativeIntegrationException If the environment variable cannot be set.
  //     */
  //    void setEnvironmentVariable(String name, String value) throws NativeIntegrationException;
  //
  //    /**
  //     * Sets the given environment variable, if possible.
  //     *
  //     * @param name The name
  //     * @param value The value
  //     * @return true if set, false if not possible.
  //     */
  //    EnvironmentModificationResult maybeSetEnvironmentVariable(String name, String value);
  //
  //    /**
  //     * Returns the working directory of the current process.
  //     *
  //     * @throws NativeIntegrationException If the process directory is not available.
  //     */
  //    File getProcessDir() throws NativeIntegrationException;
  //
  //    /**
  //     * Sets the process working directory.
  //     *
  //     * @param processDir The directory.
  //     * @throws NativeIntegrationException If process directory cannot be set.
  //     */
  //    void setProcessDir(File processDir) throws NativeIntegrationException;
  //
  //    /**
  //     * Sets the process working directory, if possible
  //     *
  //     * @param processDir The directory.
  //     * @return true if the directory can be set, false if not possible.
  //     */
  //    boolean maybeSetProcessDir(File processDir);
  //
  //    /**
  //     * Returns the OS level PID for the current process.
  //     *
  //     * @throws NativeIntegrationException If the pid is not available.
  //     */
  //    Long getPid() throws NativeIntegrationException;
  /**
   * Returns the OS level PID for the current process, or `null` if not available.
   */
  fun maybeGetPid(): Long? //
  //    /**
  //     * Detaches the current process from its terminal/console to properly put it in the background, if possible.
  //     *
  //     * @return true if the process was successfully detached.
  //     */
  //    boolean maybeDetachProcess();
  //
  //    /**
  //     * Detaches the current process from its terminal/console to properly put it in the background.
  //     *
  //     * @throws NativeIntegrationException If the process could not be detached.
  //     */
  //    void detachProcess();
}
