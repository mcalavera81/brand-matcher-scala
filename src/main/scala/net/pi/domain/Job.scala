package net.pi.domain

case class Job(jobId: Int, job: Batch)

case class JobResult(jobId: Int, result: Any)
