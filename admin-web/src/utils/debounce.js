// 常用防抖工具：避免短时间内重复触发同一逻辑。
/**
 * 防抖函数
 * @param {Function} fn - 需要防抖的函数
 * @param {number} delay - 延迟时间（毫秒），默认 500
 * @returns {Function} 防抖后的函数
 */
export function debounce(fn, delay = 500) {
    let timer = null
    return function (...args) {
        if (timer) clearTimeout(timer)
        // 保留调用方上下文，适配可能依赖 this 的场景
        const context = this
        timer = setTimeout(() => fn.apply(context, args), delay)
    }
}