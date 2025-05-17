package dev.adamko.advisoryfilelock

@Deprecated("rename", ReplaceWith("LockAccess.ReadLock"))
abstract class ReadLock internal constructor() : LockAccess
