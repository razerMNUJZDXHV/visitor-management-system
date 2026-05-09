/**
 * 防抖函数
 * @param {Function} fn - 需要防抖的函数
 * @param {number} delay - 延迟时间（毫秒），默认 500
 * @returns {Function} 防抖后的函数
 */
export function debounce(fn, delay = 500) {
    let timer = null
    return (...args) => {
        if (timer) clearTimeout(timer)
        timer = setTimeout(() => fn.apply(this, args), delay)
    }
}