package process

class DefaultProcessMetaDataProvider(
  private val environment: ProcessEnvironment
) : ProcessMetaDataProvider {

  override val processIdentifier: String
    get() {
      val pid: Long? = environment.maybeGetPid()
      return pid?.toString() ?: "gradle"
    }

  override val processDisplayName: String
    get() = "gradle"
}
