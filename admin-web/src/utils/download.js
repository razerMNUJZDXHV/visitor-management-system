// 下载工具：将二进制响应保存为本地文件。
/**
 * 触发浏览器下载（适用于 Blob/二进制响应）。
 * @param {Blob|ArrayBuffer|Uint8Array} data - 响应数据
 * @param {string} filename - 下载文件名
 */
export const downloadBlob = (data, filename) => {
  const blob = data instanceof Blob ? data : new Blob([data])
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.URL.revokeObjectURL(url)
}
